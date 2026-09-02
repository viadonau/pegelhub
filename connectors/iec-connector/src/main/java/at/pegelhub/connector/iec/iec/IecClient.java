package at.pegelhub.connector.iec.iec;

import at.pegelhub.lib.model.Measurement;

import java.util.List;
import java.util.Map;

public interface IecClient {
    void connect();
    void disconnect();

    /**
     * Sends a measurement to the IEC server for an IOA.
     *
     * @param ioa target information object address
     * @param measurement measurement to send
     */
    void sendMeasurement(int ioa, Measurement measurement);

    /**
     * Drains the currently buffered measurements, grouped by IOA.
     *
     * @return an empty map when no data is available
     * @throws InterruptedException if an implementation is interrupted while draining
     */
    Map<Integer, List<Measurement>> drainGroupedMeasurements() throws InterruptedException;
}
