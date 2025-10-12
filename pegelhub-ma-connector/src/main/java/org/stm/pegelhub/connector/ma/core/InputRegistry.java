package org.stm.pegelhub.connector.ma.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.stm.pegelhub.lib.PegelHubCommunicator;
import com.stm.pegelhub.lib.PegelHubCommunicatorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.stm.pegelhub.connector.ma.jni.RevPiReader;

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

    private final Map<Integer, PegelHubCommunicator> suppliers = new HashMap<>();
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

                            if (raw.revInput == null) {
                                throw new IllegalArgumentException("Missing fields in " + p.getFileName());
                            }

                            String revInput = raw.revInput;

                            if (!seen.add(revInput)) {
                                throw new IllegalStateException("Duplicate Input " + revInput + " in " + p.getFileName());
                            }

                            int inputOffset = this.revPiReader.resolveOffsetByName(revInput);

                            if (suppliers.containsKey(inputOffset)) {
                                throw new IllegalStateException("Duplicate resolved offset " + inputOffset +
                                        " for file " + p.getFileName());
                            }

                            PegelHubCommunicator comm = PegelHubCommunicatorFactory.create(this.coreBaseUrl, p.toString());
                            suppliers.put(inputOffset, comm);

                            log.debug("Loaded datapoint: InputName={}, ResolvedOffset={}, file={}", revInput, inputOffset, p.getFileName());
                        } catch (Exception ex) {
                            log.warn("Skipping {}: {}", p.getFileName(), ex.getMessage());
                        }
                    });
        }

        log.info("Loaded inputs from {} → suppliers={}", this.inputsDir, this.suppliers.size());
    }

    /**
     * Returns the communicator assigned to a resolved offset.
     *
     * @param offset resolved RevPi input offset
     * @return optional communicator for the offset
     */
    public Optional<PegelHubCommunicator> getSupplier(int offset) {
        return Optional.ofNullable(suppliers.get(offset));
    }

    /**
     * Returns the set of resolved input offsets currently registered.
     *
     * @return unmodifiable set of offsets
     */
    public Set<Integer> supplierOffsets() {
        return Collections.unmodifiableSet(suppliers.keySet());
    }

    private static boolean isYaml(Path p) {
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".yaml") || n.endsWith(".yml");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Raw {
        @JsonProperty("revInput")
        String revInput;
    }
}
