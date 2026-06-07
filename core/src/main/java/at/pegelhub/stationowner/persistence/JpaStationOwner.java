package at.pegelhub.stationowner.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "station_owner")
class JpaStationOwner {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 80)
    private String shortName;

    @Column(length = 2_000)
    private String notes;

    protected JpaStationOwner() {
    }

    JpaStationOwner(UUID id, String name, String shortName, String notes) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.notes = notes;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String shortName() {
        return shortName;
    }

    String notes() {
        return notes;
    }
}
