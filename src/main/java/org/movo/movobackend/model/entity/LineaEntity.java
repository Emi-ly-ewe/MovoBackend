package org.movo.movobackend.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lineas")
public class LineaEntity {
    @Id
    private String id;
    private String nombre;
    private String operador; // "TITSA" o "METRO"
}