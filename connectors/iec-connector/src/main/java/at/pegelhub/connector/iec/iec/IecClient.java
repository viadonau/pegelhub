package at.pegelhub.connector.iec.iec;

import at.pegelhub.lib.model.Measurement;

import java.util.List;
import java.util.Map;

public interface IecClient {
    /** Makes one connection attempt if unavailable; repeated calls retain a healthy connection. */
    void connect();
    /** Permanently stops this client and closes its connection; later connect calls do nothing. */
    void disconnect();

    /**
     * Send an outgoing measurement to the IEC server for a given IOA.
     *
     * @param ioa the given IOA
     * @param measurement the given Measurements to send
     */
    void sendMeasurement(int ioa, Measurement measurement);

    /**
     * Drain all received measurements currently buffered by the client and return them
     * grouped by IOA. Non-blocking - returns an empty map if no data is available.
     *
     * @return Measurements grouped by IOA
     */
    Map<Integer, List<Measurement>> drainGroupedMeasurements() throws InterruptedException;
}
