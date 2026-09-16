package at.pegelhub.connector.tstp.client;

import at.pegelhub.connector.tstp.TstpParameter;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.lib.model.Measurement;

import java.time.Instant;
import java.util.List;

public interface TstpClient extends AutoCloseable {
    List<Measurement> readMeasurements(String zrid, Instant readFrom, Instant readUntil, String unit);

    XmlQueryResponse readCatalog(int stationId, TstpParameter parameter);

    void writeMeasurements(String zrid, List<Measurement> measurements, String unit);

    @Override
    void close();
}
