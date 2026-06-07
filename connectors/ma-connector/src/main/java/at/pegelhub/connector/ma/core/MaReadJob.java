package at.pegelhub.connector.ma.core;

import at.pegelhub.lib.PegelHubCommunicator;
import at.pegelhub.lib.model.Measurement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import at.pegelhub.connector.ma.jni.RevPiReader;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;


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

                Optional<UUID> timeSeriesId = inputRegistry.getTimeSeriesId(offset);
                if (timeSeriesId.isEmpty()) {
                    log.error("Missing TimeSeries ID for offset {} at send time (unexpected).", offset);
                    return;
                }

                inputRegistry.getSupplier(offset).ifPresentOrElse(
                        communicator -> sendMeasurement(communicator, timeSeriesId.get(), now, inputValue, offset),
                        () -> log.error("Missing communicator for offset {} at send time (unexpected).", offset));
            } catch (Exception e) {
                log.error("An error occurred while trying to read from offset {}. Skipping offset.", offset, e);
            }
        });

        revPiReader.close();
    }

    private void sendMeasurement(PegelHubCommunicator communicator, UUID timeSeriesId, Instant observedAt, int inputValue, int offset) {
        try {
            Measurement measurement = new Measurement(timeSeriesId, observedAt, inputValue);
            communicator.sendMeasurements(Collections.singletonList(measurement));
            log.debug("Sent measurement from offset {}.", offset);
        } catch (Exception ex) {
            log.error("Failed sending measurements for offset {}.", offset, ex);
        }
    }
}
