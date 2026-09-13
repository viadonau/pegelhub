package at.pegelhub.notifications.transport;

import at.pegelhub.notifications.application.CredentialCatalog;
import at.pegelhub.notifications.application.NotificationProperties;
import at.pegelhub.notifications.domain.Delivery;
import at.pegelhub.notifications.domain.DestinationConfig;
import at.pegelhub.notifications.persistence.SnmpEngineRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.snmp4j.*;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.*;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SnmpDeliveryTransportTest {
    @ParameterizedTest
    @CsvSource({"v2c,authPriv,SHA,AES", "v3,authPriv,SHA,AES", "v3,authPriv,SHA256,AES256",
            "v3,authPriv,MD5,DES", "v3,authNoPriv,SHA,AES", "v3,noAuthNoPriv,SHA,AES"})
    void sendsDecodableLocalTrapsIncludingAfterEngineRestart(
            String version, String level, String auth, String privacy) throws Exception {
        var received = new LinkedBlockingQueue<PDU>();
        var transport = new DefaultUdpTransportMapping(new UdpAddress("127.0.0.1/0"));

        try (var receiver = new Snmp(transport)) {
            if (version.equals("v3")) {
                var protocols = new SecurityProtocols(SecurityProtocols.SecurityProtocolSet.maxCompatibility);
                var usm = new USM(protocols, new OctetString(MPv3.createLocalEngineID()), 0);

                OID authOid = level.equals("noAuthNoPriv") ? null : switch (auth) {
                    case "SHA" -> AuthSHA.ID;
                    case "SHA256" -> AuthHMAC192SHA256.ID;
                    default -> AuthMD5.ID;
                };
                OID privacyOid = !level.equals("authPriv") ? null : switch (privacy) {
                    case "AES" -> PrivAES128.ID;
                    case "AES256" -> PrivAES256.ID;
                    default -> PrivDES.ID;
                };

                usm.addUser(new UsmUser(new OctetString("test-user"), authOid,
                        authOid == null ? null : new OctetString("test-auth-password"), privacyOid,
                        privacyOid == null ? null : new OctetString("test-privacy-password")));

                var model = (MPv3) receiver.getMessageProcessingModel(MPv3.ID);
                model.setSecurityProtocols(protocols);
                model.setSecurityModels(new SecurityModels().addSecurityModel(usm));
            }

            receiver.addCommandResponder(new CommandResponder() {
                @Override
                public <A extends Address> void processPdu(CommandResponderEvent<A> event) {
                    received.add(event.getPDU());
                }
            });
            receiver.listen();

            var credential = new NotificationProperties.Credential(version.equals("v3") ? "SNMP_V3" : "SNMP_V2C",
                    null, 0, false, false, "test-user", null, "test-community", level, auth,
                    "test-auth-password", privacy, "test-privacy-password", true);
            var catalog = new CredentialCatalog(new NotificationProperties(true, 90, Map.of("test", credential)));

            var engines = mock(SnmpEngineRepository.class);
            byte[] id = MPv3.createLocalEngineID();
            when(engines.boot()).thenReturn(new SnmpEngineRepository.Engine(id, 1), new SnmpEngineRepository.Engine(id, 2));

            var config = new DestinationConfig("Receiver", true, DestinationConfig.Transport.SNMP, "test", null,
                    new DestinationConfig.SnmpRoute("127.0.0.1", transport.getListenAddress().getPort(), version,
                            "1.3.6.1.6.3.1.1.5.1", "1.3.6.1.4.1.65815.1.1", null));
            var now = Instant.now();
            var delivery = new Delivery(UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), config, "QA",
                    "QA: frozen water level", Delivery.State.SENDING, 1, now, now, null, null, UUID.randomUUID());

            for (int boot = 0; boot < 2; boot++) {
                new SnmpDeliveryTransport(catalog, engines).send(delivery);
                var trap = received.poll(3, TimeUnit.SECONDS);

                assertThat(trap).as("%s %s %s/%s boot %s", version, level, auth, privacy, boot).isNotNull();
                assertThat(trap.getType()).isEqualTo(PDU.NOTIFICATION);
                assertThat(trap.get(0).getOid()).isEqualTo(SnmpConstants.sysUpTime);
                assertThat(trap.get(1).getOid()).isEqualTo(SnmpConstants.snmpTrapOID);
                assertThat(trap.get(2).getVariable().toString()).isEqualTo("QA: frozen water level");
            }
        }
    }
}
