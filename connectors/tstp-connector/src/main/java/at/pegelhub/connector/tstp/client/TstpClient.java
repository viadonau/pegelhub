package at.pegelhub.connector.tstp.client;

import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.lib.model.Measurement;

import java.time.Instant;
import java.util.List;

public interface TstpClient extends AutoCloseable {
    List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil);

    XmlQueryResponse readCatalog(int stationId);

    void writeMeasurements(String zrid, List<Measurement> measurements);

    @Override
    void close();
}
