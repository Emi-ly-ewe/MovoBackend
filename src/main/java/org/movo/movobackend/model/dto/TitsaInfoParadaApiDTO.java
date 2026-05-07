package org.movo.movobackend.model.dto;

import lombok.Data;
import org.movo.movobackend.model.TitsaParada;

import java.util.List;

@Data
public class TitsaInfoParadaApiDTO {
    private boolean success;
    private TitsaParada parada;
    private List<TitsaLineaApiDTO> lineas;
}