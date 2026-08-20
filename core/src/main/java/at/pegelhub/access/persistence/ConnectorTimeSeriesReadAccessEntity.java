package at.pegelhub.access.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "connector_time_series_read_access")
@IdClass(ConnectorTimeSeriesReadAccessEntity.Key.class)
public class ConnectorTimeSeriesReadAccessEntity {
    @Id @Column(nullable = false) private UUID connectorId;
    @Id @Column(nullable = false) private UUID timeSeriesId;
    protected ConnectorTimeSeriesReadAccessEntity() { }
    public record Key(UUID connectorId, UUID timeSeriesId) implements Serializable { }
}
