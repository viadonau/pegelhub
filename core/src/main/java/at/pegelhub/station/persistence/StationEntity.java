package at.pegelhub.station.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "station")
class StationEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 200) private String waterBody;
    @Column(nullable = false, length = 8) private String status;

    protected StationEntity() { }

    StationEntity(UUID id, UUID ownerId, String name, String waterBody, String status) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.waterBody = waterBody;
        this.status = status;
    }

    UUID id() { return id; }
    UUID ownerId() { return ownerId; }
    String name() { return name; }
    String waterBody() { return waterBody; }
    String status() { return status; }
}
