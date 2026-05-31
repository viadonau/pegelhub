package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.model.Measurement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import at.pegelhub.connector.ma.jni.RevPiReader;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@AllArgsConstructor
public final class MaReadJob implements Runnable {
    private final InputRegistry inputRegistry;
    private final RevPiReader revPiReader;

    /**
     * Reads values for all registered offsets and sends measurements to the core.
     */
    @Override
    public void run() {
        Instant now = Instant.now();

        inputRegistry.supplierOffsets().forEach(offset -> {
            try {
                int inputValue = revPiReader.readFromOffset(offset);
                log.debug("Value from RevPi: {}", inputValue);

                Map<String, Double> fields = new HashMap<>();
                fields.put("value", (double) inputValue);

                Measurement measurement = new Measurement(now, fields, new HashMap<>());

                inputRegistry.getSupplier(offset).ifPresentOrElse(
                        communicator -> {
                            try {
                                communicator.sendMeasurements(Collections.singletonList(measurement));
                                log.debug("Sent measurement from offset {}.", offset);
                            } catch (Exception ex) {
                                log.error("Failed sending measurements for offset {}.", offset, ex);
                            }
                        },
                        () -> log.error("Missing communicator for offset {} at send time (unexpected).", offset)
                );
            } catch (Exception e) {
                log.error("An error occurred while trying to read from offset {}. Skipping offset.", offset, e);
            }
        });

        revPiReader.close();
    }
}
