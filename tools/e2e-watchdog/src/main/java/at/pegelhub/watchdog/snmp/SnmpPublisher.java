package at.pegelhub.watchdog.snmp;

import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.monitoring.CheckState;
import at.pegelhub.watchdog.state.StateStore;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/** Event-only ERR/OK publication after each read. UDP emission does not prove recipient delivery. */
public final class SnmpPublisher implements AutoCloseable {
    private static final long RETRY_SECONDS = 30;

    private final WatchdogConfig config;
    private final StateStore store;
    private final Snmp session;
    private final StateStore.Engine engine;
    private final String community;
    private final long startedNanos = System.nanoTime();
    private final Map<String, Instant> retryAfter = new HashMap<>();

    public SnmpPublisher(WatchdogConfig config, Path secrets, StateStore store) throws IOException {
        this.config = config;
        this.store = store;

        boolean v3 = "v3".equals(config.snmp().version());
        community = v3 ? null : WatchdogConfig.secret(secrets, config.snmp().communityFile());
        var user = v3 ? loadV3User(secrets) : null;

        // Persist the authoritative engine's new boot count before the session can emit any packet.
        engine = v3 ? store.bootEngine(MPv3.createLocalEngineID()) : null;
        session = openSession(user);
    }

    /** Publish the completed check to each receiver, retrying only when its backoff has elapsed. */
    public void publish(Instant now) {
        CheckState state = store.check();
        if (state == null || state.reason().equals("starting")) {
            return;
        }

        for (var receiver : config.snmp().receivers()) {
            publishToReceiver(state, now, receiver);
        }
    }

    @Override
    public void close() throws IOException {
        session.close();
    }

    private UsmUser loadV3User(Path secrets) throws IOException {
        String authPassword = WatchdogConfig.secret(secrets, config.snmp().authPasswordFile());
        String privacyPassword = WatchdogConfig.secret(secrets, config.snmp().privacyPasswordFile());
        if (authPassword.length() < 8 || privacyPassword.length() < 8) {
            throw new IOException("SNMPv3 passphrases require at least eight characters");
        }

        return new UsmUser(
                new OctetString(config.snmp().username()),
                AuthHMAC192SHA256.ID,
                new OctetString(authPassword),
                PrivAES256.ID,
                new OctetString(privacyPassword));
    }

    private Snmp openSession(UsmUser user) throws IOException {
        var session = new Snmp(new DefaultUdpTransportMapping());
        try {
            if (user != null) {
                configureV3(session, user);
            }
            session.listen();
            return session;
        } catch (Exception failure) {
            try {
                session.close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw new IOException("Cannot initialize SNMP transport", failure);
        }
    }

    private void configureV3(Snmp session, UsmUser user) {
        var protocols = new SecurityProtocols(SecurityProtocols.SecurityProtocolSet.none);
        protocols.addAuthenticationProtocol(new AuthHMAC192SHA256());
        protocols.addPrivacyProtocol(new PrivAES256());

        // SHA-256 provides the full 32-byte localized AES-256 key; no short-key extension is needed.
        var usm = new USM(protocols, new OctetString(engine.id()), engine.boots());
        usm.setLocalEngine(new OctetString(engine.id()), engine.boots(), 0);
        usm.addUser(user);

        var model = (MPv3) session.getMessageProcessingModel(MPv3.ID);
        model.setLocalEngineID(engine.id());
        model.setSecurityProtocols(protocols);
        model.setSecurityModels(new SecurityModels().addSecurityModel(usm));
    }

    private void publishToReceiver(CheckState state, Instant now, WatchdogConfig.Receiver receiver) {
        String receiverId = receiver.id();
        var previous = store.publication(receiverId);
        if (!isPublicationDue(state, previous)) {
            // Drop obsolete retries, but keep any persisted receiver failure until a send succeeds.
            retryAfter.remove(receiverId);
            return;
        }
        if (now.isBefore(retryAfter.getOrDefault(receiverId, Instant.MIN))) {
            return;
        }

        try {
            sendNotification(state, now, receiver);
            store.emitted(receiverId, state.signal(), now);
            retryAfter.remove(receiverId);
        } catch (IOException error) {
            store.emissionFailed(receiverId);
            retryAfter.put(receiverId, now.plusSeconds(RETRY_SECONDS));
            System.err.println("SNMP emission failed for " + receiverId + "; current state will be retried");
        }
    }

    private boolean isPublicationDue(CheckState state, StateStore.Publication previous) {
        // No startup OK or heartbeat. A failed unsent ERR followed by recovery is coalesced away.
        if (previous == null || previous.signal() == null) {
            return state.signal().equals("ERR");
        }
        return !previous.signal().equals(state.signal());
    }

    private void sendNotification(CheckState state, Instant now, WatchdogConfig.Receiver receiver) throws IOException {
        long uptimeSeconds = (System.nanoTime() - startedNanos) / 1_000_000_000L;
        if (uptimeSeconds >= Integer.MAX_VALUE) {
            throw new IOException("SNMP engine time exhausted");
        }
        session.notify(createNotification(state, now, uptimeSeconds), createTarget(receiver));
    }

    private PDU createNotification(CheckState state, Instant now, long uptimeSeconds) {
        PDU pdu = engine == null ? new PDU() : new ScopedPDU();
        if (pdu instanceof ScopedPDU scoped) {
            scoped.setContextEngineID(new OctetString(engine.id()));
        }

        pdu.setType(PDU.NOTIFICATION);
        pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks((uptimeSeconds * 100) & 0xffffffffL)));
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(config.snmp().trapOid())));
        pdu.add(new VariableBinding(
                new OID(config.snmp().messageOid()), new OctetString(formatEventMessage(state, now))));
        return pdu;
    }

    private String formatEventMessage(CheckState state, Instant now) {
        // Receiver rules parse these tokens; local refactors must preserve the wire format.
        return "PH_WATCHDOG version=1 id=" + config.watchdogId() + " status=" + state.signal()
                + " reason=" + state.reason() + " evaluatedAt=" + state.evaluatedAt()
                + " observedAt=" + state.evidenceAt() + " ageSeconds=" + state.ageSeconds() + " emittedAt=" + now;
    }

    private Target<UdpAddress> createTarget(WatchdogConfig.Receiver receiver) throws IOException {
        var address = new UdpAddress(InetAddress.getByName(receiver.host()), receiver.port());
        var target = engine == null ? createV2cTarget(address) : createV3Target(address);
        target.setRetries(0);
        target.setTimeout(1000);
        return target;
    }

    private CommunityTarget<UdpAddress> createV2cTarget(UdpAddress address) {
        var target = new CommunityTarget<>(address, new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    private UserTarget<UdpAddress> createV3Target(UdpAddress address) {
        var target = new UserTarget<UdpAddress>();
        target.setAddress(address);
        target.setVersion(SnmpConstants.version3);
        target.setSecurityName(new OctetString(config.snmp().username()));
        target.setAuthoritativeEngineID(engine.id());
        target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
        return target;
    }
}
