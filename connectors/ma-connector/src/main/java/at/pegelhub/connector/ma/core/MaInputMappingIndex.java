package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.config.MappingDirection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import at.pegelhub.connector.ma.jni.RevPiReader;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public final class MaInputMappingIndex {
    private final Map<Integer, UUID> inputs = new HashMap<>();
    private final RevPiReader revPiReader;
    private final List<InputMapping> mappings;


    /**
     * Loads inputs from YAML files, resolves RevPi offsets, and prepares communicators.
     *
     * @throws Exception if the input directory cannot be scanned or initialization fails
     */
    public void loadInputs() throws Exception {
        Set<String> seen = new HashSet<>();
        for (InputMapping mapping : mappings) {
            try {
                if (mapping.direction() != MappingDirection.EXTERNAL_TO_CORE) {
                    throw new IllegalArgumentException("mA mappings only support direction: external-to-core");
                }

                String revInput = mapping.revInput();
                UUID timeSeriesId = mapping.timeSeriesId();

                if (!seen.add(revInput)) {
                    throw new IllegalStateException("Duplicate Input " + revInput);
                }

                int inputOffset = this.revPiReader.resolveOffsetByName(revInput);

                if (inputs.containsKey(inputOffset)) {
                    throw new IllegalStateException("Duplicate resolved offset " + inputOffset);
                }

                inputs.put(inputOffset, timeSeriesId);

                log.debug("Loaded input: InputName={}, ResolvedOffset={}, TimeSeriesId={}",
                        revInput, inputOffset, timeSeriesId);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid mapping for " + mapping.revInput() + ": " + ex.getMessage(), ex);
            }
        }

        log.info("Loaded inputs -> inputs={}", this.inputs.size());
    }

    /**
     * Returns the communicator assigned to a resolved offset.
     *
     * @param offset resolved RevPi input offset
     * @return optional communicator for the offset
     */
    public Optional<UUID> getTimeSeriesId(int offset) {
        return Optional.ofNullable(inputs.get(offset));
    }

    /**
     * Returns the set of resolved input offsets currently registered.
     *
     * @return unmodifiable set of offsets
     */
    public Set<Integer> protocolOffsets() {
        return Collections.unmodifiableSet(inputs.keySet());
    }

}
