package org.movo.movobackend.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "paradas")
@IdClass(ParadaId.class)
public class ParadaEntity {
    @Id
    private String linea;
    @Id
    private Integer sentido;
    @Id
    @Column(name = "parada_id")
    private String paradaId;
    private String nombre;
    @Column(name = "orden_parada")
    private Integer ordenParada;
    private Double lat;
    private Double lng;
    private String operador;
}