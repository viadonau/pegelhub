package at.pegelhub.watchdog.snmp;

import at.pegelhub.watchdog.Json;
import at.pegelhub.watchdog.config.WatchdogConfig;
import at.pegelhub.watchdog.lab.TrapReceiver;
import at.pegelhub.watchdog.monitoring.CheckState;
import at.pegelhub.watchdog.monitoring.Checks;
import at.pegelhub.watchdog.state.StateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static at.pegelhub.watchdog.WatchdogFixtures.START;
import static at.pegelhub.watchdog.WatchdogFixtures.config;
import static org.junit.jupiter.api.Assertions.*;

class SnmpPublisherTest {
    @TempDir
    Path directory;

    @Test
    void onlyErrorsAndSubsequentRecoveryAreSentToBothReceiversWithoutHeartbeat() throws Exception {
        writeSecrets();
        try (var first = new TrapReceiver(0);
             var second = new TrapReceiver(0);
             var store = new StateStore(directory, config(1162, "v2c"))) {
            Instant now = START;
            store.beginSession(now);
            var config = config(List.of(new WatchdogConfig.Receiver("127.0.0.1", first.port()),
                    new WatchdogConfig.Receiver("127.0.0.1", second.port())), "v2c");
            try (var publisher = new SnmpPublisher(config, directory, store)) {
                publisher.publish(now);
                store.record(Checks.evaluate(now, now, Duration.ofSeconds(600)));
                publisher.publish(now);
                assertNull(first.await(150), "A healthy startup is not a recovery event");

                store.record(Checks.evaluate(now, now.plusSeconds(601), Duration.ofSeconds(600)));
                publisher.publish(now.plusSeconds(601));
                var packet = first.awaitPacket();
                assertPacket(packet, "v2c", "noAuthNoPriv");
                assertMessage(packet.payload(), "ERR", "stale");
                assertMessage(second.await(), "ERR", "stale");
                publisher.publish(now.plusSeconds(1800));
                assertNull(first.await(150), "There must be no periodic ERR refresh");
                store.record(CheckState.unknown("read_failed", now.plusSeconds(1801)));
                publisher.publish(now.plusSeconds(1801));
                assertNull(first.await(150), "An unavailable read must not acknowledge the stale alarm");

                store.record(Checks.evaluate(now.plusSeconds(1802), now.plusSeconds(1802), Duration.ofSeconds(600)));
                publisher.publish(now.plusSeconds(1802));
                assertMessage(first.await(), "OK", "fresh");
                assertMessage(second.await(), "OK", "fresh");
                publisher.publish(now.plusSeconds(2400));
                assertNull(first.await(150), "There must be no periodic OK refresh");
            }
        }
    }

    @Test
    void failedReadsSendErrAndOneFailedReceiverDoesNotPreventTheOther() throws Exception {
        writeSecrets();
        try (var receiver = new TrapReceiver(0);
             var store = new StateStore(directory, config(1162, "v2c"))) {
            Instant now = START;
            store.beginSession(now);
            var config = config(List.of(new WatchdogConfig.Receiver("256.256.256.256", 1162),
                    new WatchdogConfig.Receiver("127.0.0.1", receiver.port())), "v2c");
            try (var publisher = new SnmpPublisher(config, directory, store)) {
                store.record(CheckState.unknown("read_failed", now));
                publisher.publish(now);
                assertMessage(receiver.await(), "ERR", "read_failed");
                assertEquals(false, StateStore.inspect(directory, now).get("healthy"));
                publisher.publish(now.plusSeconds(31));
                assertNull(receiver.await(150), "Only the failed receiver should be retried");

                store.record(Checks.evaluate(now.plusSeconds(32), now.plusSeconds(32), Duration.ofSeconds(600)));
                publisher.publish(now.plusSeconds(32));
                assertMessage(receiver.await(), "OK", "fresh");
                assertEquals(false, StateStore.inspect(directory, now.plusSeconds(32)).get("healthy"),
                        "Route recovery cannot repair a known local receiver failure");
            }
        }
    }

    @Test
    void v3UsesSha256Aes256AndPersistsEngineAndAlarmAcrossRestart() throws Exception {
        writeSecrets();
        String identity;
        try (var receiver = new TrapReceiver(0)) {
            var config = config(receiver.port(), "v3");
            try (var store = new StateStore(directory, config)) {
                store.beginSession(START);
                try (var publisher = new SnmpPublisher(config, directory, store)) {
                    store.record(CheckState.unknown("read_failed", START));
                    publisher.publish(START);
                    var packet = receiver.awaitPacket();
                    assertPacket(packet, "v3", "authPriv");
                    assertMessage(packet.payload(), "ERR", "read_failed");
                    var state = (Map<?, ?>) StateStore.inspect(directory, START).get("state");
                    assertFalse(state.containsKey("identity"));
                    assertFalse(Json.CODEC.toJson(state).contains("password"));
                    var engine = (Map<?, ?>) state.get("snmpEngine");
                    assertEquals(1, engine.get("boots"));
                    identity = (String) engine.get("id");
                }
            }
            try (var store = new StateStore(directory, config)) {
                store.beginSession(START.plusSeconds(1));
                try (var publisher = new SnmpPublisher(config, directory, store)) {
                    publisher.publish(START.plusSeconds(1));
                    assertNull(receiver.await(150), "Restart is not recovery");
                    store.record(CheckState.unknown("read_failed", START.plusSeconds(2)));
                    publisher.publish(START.plusSeconds(2));
                    assertNull(receiver.await(150), "An already emitted error survives restart");
                    store.record(Checks.evaluate(
                            START, START.plusSeconds(3), Duration.ofSeconds(600)));
                    publisher.publish(START.plusSeconds(3));
                    assertMessage(receiver.await(), "OK", "fresh");
                }
                var state = (Map<?, ?>) StateStore.inspect(directory, START.plusSeconds(3)).get("state");
                var engine = (Map<?, ?>) state.get("snmpEngine");
                assertEquals(2, engine.get("boots"));
                assertEquals(identity, engine.get("id"));
            }
        }
    }

    private void writeSecrets() throws Exception {
        Files.writeString(directory.resolve("community"), "private-community");
        Files.writeString(directory.resolve("auth"), "test-auth-password");
        Files.writeString(directory.resolve("privacy"), "test-privacy-password");
    }

    private void assertPacket(TrapReceiver.ReceivedTrap packet, String version, String security) {
        assertNotNull(packet, "SNMP packet was not decoded");
        assertEquals(version, packet.version());
        assertEquals(security, packet.securityLevel());
        assertEquals(3, packet.bindings().size());
        assertEquals("1.3.6.1.2.1.1.3.0", packet.bindings().getFirst().oid());
        assertEquals("TimeTicks", packet.bindings().getFirst().type());
        assertEquals("1.3.6.1.6.3.1.1.4.1.0", packet.bindings().get(1).oid());
        assertEquals("1.3.6.1.4.1.32473.1", packet.bindings().get(1).value());
        assertEquals("1.3.6.1.4.1.32473.2", packet.bindings().get(2).oid());
        assertEquals(packet.payload(), packet.bindings().get(2).value());
        String json = Json.CODEC.toJson(packet);
        assertFalse(json.contains("private-community"));
        assertFalse(json.contains("password"));
    }

    private void assertMessage(String message, String signal, String reason) {
        assertNotNull(message, "SNMP packet was not decoded");
        String expectedPrefix = "PH_WATCHDOG version=1 id=test status=" + signal + " reason=" + reason + " ";
        assertTrue(message.startsWith(expectedPrefix), message);
        assertFalse(message.contains("password"));
        assertFalse(message.contains("private-community"));
    }
}
