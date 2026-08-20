package at.pegelhub.access.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConnectorStationReadAccessRepository extends JpaRepository<ConnectorStationReadAccessEntity, ConnectorStationReadAccessEntity.Key> {
    @Modifying
    @Query(value = "insert into connector_station_read_access (connector_id, station_id) values (:connectorId, :stationId) on conflict do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("connectorId") UUID connectorId, @Param("stationId") UUID stationId);

    boolean existsByConnectorIdAndStationId(UUID connectorId, UUID stationId);
    void deleteByConnectorIdAndStationId(UUID connectorId, UUID stationId);
}
