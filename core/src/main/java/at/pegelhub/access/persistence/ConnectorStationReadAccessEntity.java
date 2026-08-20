package at.pegelhub.access.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "connector_station_read_access")
@IdClass(ConnectorStationReadAccessEntity.Key.class)
public class ConnectorStationReadAccessEntity {
    @Id @Column(nullable = false) private UUID connectorId;
    @Id @Column(nullable = false) private UUID stationId;
    protected ConnectorStationReadAccessEntity() { }
    public record Key(UUID connectorId, UUID stationId) implements Serializable { }
}
