package at.pegelhub.notifications.transport;

import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties.Credential;
import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.DestinationConfig.SnmpRoute;
import at.pegelhub.notifications.persistence.SnmpEngineRepository;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class SnmpDeliveryTransport {

    private final CredentialCatalog credentials;
    private final SnmpEngineRepository engines;

    private SnmpEngineRepository.Engine engine;
    private long started = System.nanoTime();

    public SnmpDeliveryTransport(CredentialCatalog credentials, SnmpEngineRepository engines) {
        this.credentials = credentials;
        this.engines = engines;
    }

    /**
     * Sends an unacknowledged trap, not an INFORM; normal return cannot prove receiver acceptance.
     * Synchronization keeps the lazy, process-wide SNMPv3 engine boot initialization single-shot.
     */
    public synchronized void send(Delivery delivery) {
        var credential = credentials.resolve(delivery.route());
        var route = delivery.route().snmp();
        boolean v3 = route.version().equals("v3");

        if (v3) {
            initializeV3Engine();
        }

        try (var session = new Snmp(new DefaultUdpTransportMapping())) {
            if (v3) {
                configureV3(session, credential);
            }

            session.listen();
            session.notify(notification(delivery, v3), target(route, credential, v3));
        } catch (IOException exception) {
            throw new UncheckedIOException("SNMP transport failed", exception);
        }
    }

    private void initializeV3Engine() {
        if (engine == null) {
            // Commit the increment before sending: reusing a boot count after restart breaks replay protection.
            engine = engines.boot();
            started = System.nanoTime();
        }

        if (engine.boots() == Integer.MAX_VALUE || elapsedSeconds() >= Integer.MAX_VALUE) {
            throw new IllegalStateException("SNMP engine state is exhausted");
        }
    }

    private PDU notification(Delivery delivery, boolean v3) {
        var route = delivery.route().snmp();

        PDU pdu = new PDU();
        if (v3) {
            var scoped = new ScopedPDU();
            scoped.setContextEngineID(new OctetString(engine.id()));
            pdu = scoped;
        }

        pdu.setType(PDU.NOTIFICATION);
        pdu.add(new VariableBinding(SnmpConstants.sysUpTime, new TimeTicks((elapsedSeconds() * 100) & 0xffffffffL)));
        pdu.add(new VariableBinding(SnmpConstants.snmpTrapOID, new OID(route.trapOid())));
        if (route.enterpriseOid() != null && !route.enterpriseOid().isBlank()) {
            pdu.add(new VariableBinding(SnmpConstants.snmpTrapEnterprise, new OID(route.enterpriseOid())));
        }
        pdu.add(new VariableBinding(new OID(route.messageOid()), new OctetString(delivery.body())));

        return pdu;
    }

    private Target<Address> target(SnmpRoute route, Credential credential, boolean v3) {
        Address address = GenericAddress.parse("udp:" + route.host() + "/" + route.port());
        if (address == null) {
            throw new IllegalStateException("Invalid SNMP address");
        }

        Target<Address> target;
        if (v3) {
            var user = new UserTarget<Address>();
            user.setAddress(address);
            user.setVersion(SnmpConstants.version3);
            user.setSecurityName(new OctetString(credential.username()));
            user.setAuthoritativeEngineID(engine.id());
            user.setSecurityLevel(level(credential));
            target = user;
        } else {
            target = new CommunityTarget<>(address, new OctetString(credential.community()));
            target.setVersion(SnmpConstants.version2c);
        }

        // The durable queue owns retries; the transport must not add hidden attempts.
        target.setRetries(0);
        target.setTimeout(10_000);

        return target;
    }

    private long elapsedSeconds() {
        return started == 0 ? 0 : (System.nanoTime() - started) / 1_000_000_000L;
    }

    private void configureV3(Snmp session, Credential credential) {
        var protocols = new SecurityProtocols(SecurityProtocols.SecurityProtocolSet.none);
        protocols.addAuthenticationProtocol(new AuthSHA());
        protocols.addAuthenticationProtocol(new AuthHMAC192SHA256());
        protocols.addPrivacyProtocol(new PrivAES128());
        protocols.addPrivacyProtocol(new PrivAES256());
        if (credential.allowLegacyAlgorithms()) {
            protocols.addAuthenticationProtocol(new AuthMD5());
            protocols.addPrivacyProtocol(new PrivDES());
        }

        var usm = new USM(protocols, new OctetString(engine.id()), engine.boots());
        usm.setLocalEngine(new OctetString(engine.id()), engine.boots(), (int) elapsedSeconds());

        OID auth = level(credential) == SecurityLevel.NOAUTH_NOPRIV ? null : switch (credential.authProtocol()) {
            case "SHA" -> AuthSHA.ID;
            case "SHA256" -> AuthHMAC192SHA256.ID;
            case "MD5" -> AuthMD5.ID;
            default -> throw new IllegalStateException("Unsupported SNMP authentication");
        };

        OID privacy = level(credential) != SecurityLevel.AUTH_PRIV ? null : switch (credential.privProtocol()) {
            case "AES" -> PrivAES128.ID;
            case "AES256" -> PrivAES256.ID;
            case "DES" -> PrivDES.ID;
            default -> throw new IllegalStateException("Unsupported SNMP privacy");
        };

        usm.addUser(new UsmUser(new OctetString(credential.username()), auth,
                auth == null ? null : new OctetString(credential.authPassword()), privacy,
                privacy == null ? null : new OctetString(credential.privPassword())));

        var model = (MPv3) session.getMessageProcessingModel(MPv3.ID);
        model.setLocalEngineID(engine.id());
        model.setSecurityProtocols(protocols);

        // Keep credentials and legacy-algorithm opt-ins local to this session, not SNMP4J's global registries.
        model.setSecurityModels(new SecurityModels().addSecurityModel(usm));
    }

    private static int level(Credential credential) {
        return switch (credential.securityLevel()) {
            case "authPriv" -> SecurityLevel.AUTH_PRIV;
            case "authNoPriv" -> SecurityLevel.AUTH_NOPRIV;
            default -> SecurityLevel.NOAUTH_NOPRIV;
        };
    }
}
