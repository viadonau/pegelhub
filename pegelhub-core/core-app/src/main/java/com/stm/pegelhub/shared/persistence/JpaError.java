package com.stm.pegelhub.shared.persistence;

import com.stm.pegelhub.taker.persistence.JpaTakerServiceManufacturer;

import lombok.Data;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "Error")
public class JpaError extends IdentifiableEntity{
    @Column(nullable = false)
    private String errorCode;

    @Column(nullable = false)
    private String plaintext;

    @Column(nullable = false)
    private JpaTakerServiceManufacturer takerServiceManufacturer;
}
