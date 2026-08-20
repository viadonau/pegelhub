package at.pegelhub.access.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ConnectorTimeSeriesReadAccessRepository extends JpaRepository<ConnectorTimeSeriesReadAccessEntity, ConnectorTimeSeriesReadAccessEntity.Key> {
    @Modifying
    @Query(value = "insert into connector_time_series_read_access (connector_id, time_series_id) values (:connectorId, :timeSeriesId) on conflict do nothing", nativeQuery = true)
    int insertIfAbsent(@Param("connectorId") UUID connectorId, @Param("timeSeriesId") UUID timeSeriesId);

    boolean existsByConnectorIdAndTimeSeriesId(UUID connectorId, UUID timeSeriesId);
    void deleteByConnectorIdAndTimeSeriesId(UUID connectorId, UUID timeSeriesId);
}
