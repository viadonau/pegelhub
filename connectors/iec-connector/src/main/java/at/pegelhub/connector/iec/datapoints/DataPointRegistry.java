package at.pegelhub.connector.iec.datapoints;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.PegelHubCommunicatorFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class DataPointRegistry {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Map<Integer, PegelHubCommunicator> suppliers = new HashMap<>();
    private final Map<Integer, PegelHubCommunicator> takers = new HashMap<>();
    private final Map<Integer, UUID> timeSeriesIds = new HashMap<>();

    public DataPointRegistry(String dataPointsDir, URL coreBaseUrl) throws Exception {
        Objects.requireNonNull(dataPointsDir, "dataPointsDir");
        Path dir = Paths.get(dataPointsDir);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("DataPointsDir is not a directory: " + dir.toAbsolutePath());
        }
        loadDataPoints(dir, coreBaseUrl);
        log.info("Loaded datapoints from {} → suppliers={}, takers={}", dir.toAbsolutePath(), suppliers.size(), takers.size());
    }

    public Optional<PegelHubCommunicator> getSupplier(int ioa) {
        return Optional.ofNullable(suppliers.get(ioa));
    }

    public Optional<PegelHubCommunicator> getTaker(int ioa) {
        return Optional.ofNullable(takers.get(ioa));
    }

    public Optional<UUID> getTimeSeriesId(int ioa) {
        return Optional.ofNullable(timeSeriesIds.get(ioa));
    }

    public Set<Integer> supplierIoas() {
        return Collections.unmodifiableSet(suppliers.keySet());
    }

    public Set<Integer> takerIoas() {
        return Collections.unmodifiableSet(takers.keySet());
    }

    private void loadDataPoints(Path dir, URL coreBaseUrl) throws Exception {
        Set<Integer> seen = new HashSet<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(Files::isRegularFile)
                    .filter(DataPointRegistry::isYaml)
                    .forEach(p -> {
                        try {
                            Raw raw = YAML.readValue(p.toFile(), Raw.class);

                            if (raw.iecIoa == null || raw.isSupplier == null || raw.timeSeriesId == null) {
                                throw new IllegalArgumentException("Missing fields in " + p.getFileName());
                            }

                            int ioa = raw.iecIoa;

                            if (!seen.add(ioa)) {
                                throw new IllegalStateException("Duplicate IOA " + ioa + " in " + p.getFileName());
                            }

                            PegelHubCommunicator comm = PegelHubCommunicatorFactory.create(coreBaseUrl, p.toString());

                            if (raw.isSupplier) {
                                suppliers.put(ioa, comm);
                            } else {
                                takers.put(ioa, comm);
                            }
                            timeSeriesIds.put(ioa, raw.timeSeriesId);

                            log.debug("Loaded datapoint: IOA={}, timeSeriesId={}, isSupplier={}, file={}",
                                    ioa, raw.timeSeriesId, raw.isSupplier, p.getFileName());
                        } catch (Exception ex) {
                            log.warn("Skipping {}: {}", p.getFileName(), ex.getMessage());
                        }
                    });
        }
    }

    private static boolean isYaml(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".yaml") || n.endsWith(".yml");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Raw {
        @JsonProperty("iecIOA")
        Integer iecIoa;
        @JsonProperty("isSupplier")
        Boolean isSupplier;
        @JsonProperty("timeSeriesId")
        UUID timeSeriesId;
    }
}
