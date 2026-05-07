package org.movo.movobackend.model.response;

import lombok.Data;
import org.movo.movobackend.model.TitsaParada;

@Data
public class TitsaParadaResponse {
    private boolean success;
    private TitsaParada parada;
}
