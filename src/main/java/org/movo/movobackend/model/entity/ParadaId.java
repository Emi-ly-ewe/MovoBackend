package org.movo.movobackend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParadaId implements Serializable {
    private String linea;
    private Integer sentido;
    private String paradaId;
}