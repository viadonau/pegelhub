package at.pegelhub.connector.ma.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.PegelHubCommunicatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import at.pegelhub.connector.ma.jni.RevPiReader;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class InputRegistry {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private final Map<Integer, InputRegistration> inputs = new HashMap<>();
    private final RevPiReader revPiReader;
    private final String inputsDir;
    private final URL coreBaseUrl;


    /**
     * Loads inputs from YAML files, resolves RevPi offsets, and prepares communicators.
     *
     * @throws Exception if the input directory cannot be scanned or initialization fails
     */
    public void loadInputs() throws Exception {
        Set<String> seen = new HashSet<>();
        try (Stream<Path> files = Files.list(Paths.get(this.inputsDir))) {
            files.filter(Files::isRegularFile)
                    .filter(InputRegistry::isYaml)
                    .forEach(p -> {
                        try {
                            Raw raw = YAML.readValue(p.toFile(), Raw.class);

                            if (raw.revInput == null || raw.timeSeriesId == null) {
                                throw new IllegalArgumentException("Missing fields in " + p.getFileName());
                            }

                            String revInput = raw.revInput;
                            UUID timeSeriesId = raw.timeSeriesId;

                            if (!seen.add(revInput)) {
                                throw new IllegalStateException("Duplicate Input " + revInput + " in " + p.getFileName());
                            }

                            int inputOffset = this.revPiReader.resolveOffsetByName(revInput);

                            if (inputs.containsKey(inputOffset)) {
                                throw new IllegalStateException("Duplicate resolved offset " + inputOffset +
                                        " for file " + p.getFileName());
                            }

                            PegelHubCommunicator comm = PegelHubCommunicatorFactory.create(this.coreBaseUrl, p.toString());
                            inputs.put(inputOffset, new InputRegistration(comm, timeSeriesId));

                            log.debug("Loaded input: InputName={}, ResolvedOffset={}, TimeSeriesId={}, file={}",
                                    revInput, inputOffset, timeSeriesId, p.getFileName());
                        } catch (Exception ex) {
                            log.warn("Skipping {}: {}", p.getFileName(), ex.getMessage());
                        }
                    });
        }

        log.info("Loaded inputs from {} -> inputs={}", this.inputsDir, this.inputs.size());
    }

    /**
     * Returns the communicator assigned to a resolved offset.
     *
     * @param offset resolved RevPi input offset
     * @return optional communicator for the offset
     */
    public Optional<PegelHubCommunicator> getSupplier(int offset) {
        return Optional.ofNullable(inputs.get(offset)).map(InputRegistration::communicator);
    }

    public Optional<UUID> getTimeSeriesId(int offset) {
        return Optional.ofNullable(inputs.get(offset)).map(InputRegistration::timeSeriesId);
    }

    /**
     * Returns the set of resolved input offsets currently registered.
     *
     * @return unmodifiable set of offsets
     */
    public Set<Integer> supplierOffsets() {
        return Collections.unmodifiableSet(inputs.keySet());
    }

    private static boolean isYaml(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".yaml") || n.endsWith(".yml");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Raw {
        @JsonProperty("revInput")
        String revInput;

        @JsonProperty("timeSeriesId")
        UUID timeSeriesId;
    }

    private record InputRegistration(PegelHubCommunicator communicator, UUID timeSeriesId) {
    }
}
