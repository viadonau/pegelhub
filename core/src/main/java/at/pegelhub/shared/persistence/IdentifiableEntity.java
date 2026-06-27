package at.pegelhub.shared.persistence;


import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.UUID;

/**
 * A non-persisted superclass for JPA Data classes, ensuring IDs for subclasses.
 */

@Data
@MappedSuperclass
public abstract class IdentifiableEntity implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    protected UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
