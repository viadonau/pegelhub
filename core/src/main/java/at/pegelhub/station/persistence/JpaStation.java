package at.pegelhub.station.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "station")
class JpaStation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false, unique = true, length = 80)
    private String stationNumber;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 200)
    private String waterBody;

    @Column(length = 500)
    private String location;

    protected JpaStation() {
    }

    JpaStation(UUID id, UUID ownerId, String stationNumber, String name, String waterBody, String location) {
        this.id = id;
        this.ownerId = ownerId;
        this.stationNumber = stationNumber;
        this.name = name;
        this.waterBody = waterBody;
        this.location = location;
    }

    UUID id() {
        return id;
    }

    UUID ownerId() {
        return ownerId;
    }

    String stationNumber() {
        return stationNumber;
    }

    String name() {
        return name;
    }

    String waterBody() {
        return waterBody;
    }

    String location() {
        return location;
    }
}
