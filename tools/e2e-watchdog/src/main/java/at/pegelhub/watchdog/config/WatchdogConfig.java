package at.pegelhub.watchdog.config;

import at.pegelhub.lib.config.CoreAuthentication;
import at.pegelhub.lib.config.CoreConnection;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.snmp4j.smi.OID;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** One return series, one age limit, and explicitly configured receivers. Secrets remain file references. */
public record WatchdogConfig(
        String watchdogId,
        Core core,
        UUID timeSeriesId,
        long silenceSeconds,
        Long pollSeconds,
        Snmp snmp) {

    private static final long MAX_SILENCE_SECONDS = 604_800;

    public record Core(String baseUrl, String tokenUrl, String clientId, String clientSecretFile) {
    }

    public record Receiver(String host, int port) {
        public Receiver {
            if (host == null || !host.matches("[a-zA-Z0-9.-]+") || port < 1 || port > 65535) {
                fail("SNMP host and port required");
            }
        }

        public String id() {
            return host + ":" + port;
        }
    }

    public record Snmp(
            List<Receiver> receivers,
            String version,
            String trapOid,
            String messageOid,
            String communityFile,
            String username,
            String authPasswordFile,
            String privacyPasswordFile) {

        public Snmp {
            validateReceivers(receivers);
            receivers = List.copyOf(receivers);

            if (!"v2c".equals(version) && !"v3".equals(version)) {
                fail("SNMP version must be v2c or v3");
            }
            if (!isValidOid(trapOid) || !isValidOid(messageOid)) {
                fail("Numeric SNMP OIDs required");
            }
            if ("v3".equals(version) && (username == null || username.isBlank() || username.length() > 32)) {
                fail("SNMPv3 username required (maximum 32 characters)");
            }
        }

        private static void validateReceivers(List<Receiver> receivers) {
            if (receivers == null || receivers.isEmpty() || receivers.size() > 2
                    || receivers.stream().anyMatch(Objects::isNull)
                    || new HashSet<>(receivers).size() != receivers.size()) {
                fail("Select one or two distinct SNMP receivers");
            }
        }
    }

    public WatchdogConfig {
        if (watchdogId == null || !watchdogId.matches("[a-zA-Z0-9_-]{1,64}")) {
            fail("watchdogId is required");
        }
        validateCore(core);

        if (timeSeriesId == null) {
            fail("Return timeSeriesId is required");
        }
        if (pollSeconds == null) {
            pollSeconds = 15L;
        }

        validateTiming(silenceSeconds, pollSeconds);
        if (snmp == null) {
            fail("SNMP configuration required");
        }
    }

    /** YAML errors may echo input. Only our own validation messages are safe to expose in diagnostics. */
    public static WatchdogConfig load(Path file) throws IOException {
        try {
            return new YAMLMapper().readValue(file.toFile(), WatchdogConfig.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw sanitizedFailure(failure);
        }
    }

    public CoreConnection connection(Path root) throws IOException {
        return new CoreConnection(
                URI.create(core.baseUrl()).toURL(),
                new CoreAuthentication(core.tokenUrl(), core.clientId(), secret(root, core.clientSecretFile())));
    }

    public Duration silence() {
        return Duration.ofSeconds(silenceSeconds);
    }

    /** Bind alarms to their route and receivers, not to tunable polling or freshness policy. */
    public String identity() {
        return String.join("|", "return-freshness-v2", routeIdentity(), receiverIdentity());
    }

    private String routeIdentity() {
        return String.join("|", watchdogId, core.baseUrl(), core.tokenUrl(), core.clientId(), timeSeriesId.toString());
    }

    private String receiverIdentity() {
        String receivers = snmp.receivers().stream().map(Receiver::id).sorted().toList().toString();
        return String.join("|", snmp.version(), snmp.trapOid(), snmp.messageOid(),
                String.valueOf(snmp.username()), receivers);
    }

    /** Resolve a bounded secret file relative to configuration; callers must not persist or expose its contents. */
    public static String secret(Path root, String reference) throws IOException {
        if (reference == null || reference.isBlank()) {
            throw new IOException("Missing secret file reference");
        }

        var path = root.resolve(reference);
        if (Files.size(path) > 16_384) {
            throw new IOException("Secret file exceeds size limit");
        }
        String value = Files.readString(path).strip();
        if (value.isEmpty()) {
            throw new IOException("Secret file is empty");
        }
        return value;
    }

    private static ConfigurationFailure sanitizedFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConfigurationFailure safe) {
                return safe;
            }
        }
        return new ConfigurationFailure("Invalid configuration document: check field names and value types");
    }

    private static void validateCore(Core core) {
        if (core == null || core.clientId() == null || core.clientId().isBlank()) {
            fail("Core client is required");
        }
        validateEndpoint(core.baseUrl());
        validateEndpoint(core.tokenUrl());
    }

    private static void validateTiming(long silenceSeconds, long pollSeconds) {
        // No production age default: the full route's polling delays must fit this limit.
        if (silenceSeconds < 1 || silenceSeconds > MAX_SILENCE_SECONDS) {
            fail("silenceSeconds must be between 1 and 604800");
        }
        if (pollSeconds < 1 || pollSeconds > 60) {
            fail("pollSeconds must be between 1 and 60");
        }
    }

    private static void validateEndpoint(String value) {
        if (value == null) {
            fail("Core endpoints required");
        }

        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException failure) {
            throw new ConfigurationFailure("Invalid Core endpoint");
        }
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
            fail("Invalid Core endpoint");
        }
    }

    private static boolean isValidOid(String value) {
        return value != null && value.matches("[0-9]+(\\.[0-9]+)+") && new OID(value).isValid();
    }

    private static void fail(String message) {
        throw new ConfigurationFailure(message);
    }
}
