package org.movo.movobackend.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.movo.movobackend.model.TitsaParada;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TitsaParadaResponse {
    private boolean success;
    private TitsaParada parada;
    private List<TitsaParada> llegadas;
}