package org.movo.movobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroParada {
    private int idParada;      // stopSAE
    private String nombre;     // stopDescription
    private int orden;        // orderStop
}
