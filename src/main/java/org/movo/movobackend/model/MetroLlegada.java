package org.movo.movobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroLlegada {
    private int linea;          // route
    private String destino;     // destinationStopDescription
    private int minutos;        // remainingMinutes
}
