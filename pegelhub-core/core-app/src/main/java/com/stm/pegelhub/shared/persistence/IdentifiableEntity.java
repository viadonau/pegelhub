package com.stm.pegelhub.shared.persistence;


import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
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
}
