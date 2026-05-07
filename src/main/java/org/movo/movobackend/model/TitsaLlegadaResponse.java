package org.movo.movobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TitsaLlegadaResponse {
    private boolean success;
    private TitsaParada parada;
    private List<TitsaLlegada> llegadas;
}