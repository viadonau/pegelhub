package at.pegelhub.watchdog.lab;

import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.AuthHMAC192SHA256;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/** Real decoder shared by UDP tests and the manual lab, including authenticated/encrypted v3 packets. */
public final class TrapReceiver implements AutoCloseable {
    private final Snmp snmp;
    private final DefaultUdpTransportMapping transport;
    private final LinkedBlockingDeque<ReceivedTrap> packets = new LinkedBlockingDeque<>(100);

    public record Binding(String oid, String type, String value) {
    }

    /** Decoded application data only, not raw bytes or the SNMP security envelope. */
    public record ReceivedTrap(
            String receivedAt,
            int receiverPort,
            String peerAddress,
            String version,
            String securityLevel,
            String pduType,
            String payload,
            List<Binding> bindings) {
    }

    public TrapReceiver(int port) throws IOException {
        transport = new DefaultUdpTransportMapping(new UdpAddress("0.0.0.0/" + port));
        snmp = new Snmp(transport);

        var protocols = new SecurityProtocols(SecurityProtocols.SecurityProtocolSet.none);
        protocols.addAuthenticationProtocol(new AuthHMAC192SHA256());
        protocols.addPrivacyProtocol(new PrivAES256());
        var engine = new OctetString(MPv3.createLocalEngineID());
        var usm = new USM(protocols, engine, 0);
        usm.addUser(new UsmUser(new OctetString("test"), AuthHMAC192SHA256.ID, new OctetString("test-auth-password"),
                PrivAES256.ID, new OctetString("test-privacy-password")));
        var model = (MPv3) snmp.getMessageProcessingModel(MPv3.ID);
        model.setSecurityProtocols(protocols);
        model.setSecurityModels(new SecurityModels().addSecurityModel(usm));

        snmp.addCommandResponder(new CommandResponder() {
            @Override
            public <A extends Address> void processPdu(CommandResponderEvent<A> event) {
                if (event.getPDU() == null || event.getPDU().size() < 3) {
                    return;
                }

                var packet = describe(event);
                if (!packets.offer(packet)) {
                    packets.poll();
                    packets.offer(packet);
                }
                event.setProcessed(true);
            }
        });
        snmp.listen();
    }

    public int port() {
        return transport.getListenAddress().getPort();
    }

    private ReceivedTrap describe(CommandResponderEvent<?> event) {
        String version = switch (event.getMessageProcessingModel()) {
            case MPv2c.ID -> "v2c";
            case MPv3.ID -> "v3";
            default -> "unknown";
        };
        String security = switch (event.getSecurityLevel()) {
            case SecurityLevel.NOAUTH_NOPRIV -> "noAuthNoPriv";
            case SecurityLevel.AUTH_NOPRIV -> "authNoPriv";
            case SecurityLevel.AUTH_PRIV -> "authPriv";
            default -> "unknown";
        };
        var pdu = event.getPDU();
        var bindings = pdu.getVariableBindings().stream()
                .map(binding -> new Binding(binding.getOid().toString(),
                        binding.getVariable().getSyntaxString(), binding.getVariable().toString()))
                .toList();

        // Do not expose event.securityName: for v2c it contains the community secret.
        return new ReceivedTrap(Instant.now().toString(), port(), event.getPeerAddress().toString(),
                version, security, PDU.getTypeString(pdu.getType()), bindings.get(2).value(), bindings);
    }

    public List<ReceivedTrap> packets() {
        return List.copyOf(packets);
    }

    public List<String> messages() {
        return packets.stream().map(ReceivedTrap::payload).toList();
    }

    public ReceivedTrap awaitPacket() throws InterruptedException {
        return packets.poll(5, TimeUnit.SECONDS);
    }

    public String await() throws InterruptedException {
        return await(5_000);
    }

    public String await(long millis) throws InterruptedException {
        var packet = packets.poll(millis, TimeUnit.MILLISECONDS);
        return packet == null ? null : packet.payload();
    }

    @Override
    public void close() throws IOException {
        snmp.close();
    }
}
