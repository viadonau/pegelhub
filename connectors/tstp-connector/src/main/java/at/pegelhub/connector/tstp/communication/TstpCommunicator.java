package at.pegelhub.connector.tstp.communication;

import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.lib.model.Measurement;

import java.time.Instant;
import java.util.List;

/**
 * Handles the communication with the TSTP-Server
 */
public interface TstpCommunicator {
    /**
     * Get measurements from the TSTP-Server
     *
     * @param zrid the ZRID to identify the time series
     * @param readFrom the start point to read entries
     * @param readUntil the endpoint to stop reading entries
     * @return a list of measurements returned from the TSTP-Server
     */
    List<Measurement> getMeasurements(String zrid, Instant readFrom, Instant readUntil);

    /**
     * Get the ZRID Catalog for a certain location with the DBMS Number from the TSTP-Server
     *
     * @param dbms the DBMS Number from the location
     * @return the catalog returned from the TSTP-Server
     */
    XmlQueryResponse getCatalog(int dbms);

    /**
     * Send a list of measurements to the TSTP-Server
     *
     * @param zrid the ZRID of the time series to write the measurements to
     * @param measurements the measurements to send
     */
    void sendMeasurements(String zrid, List<Measurement> measurements);
}
