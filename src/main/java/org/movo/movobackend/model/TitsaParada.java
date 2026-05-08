package org.movo.movobackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TitsaParada {

    // --- DATOS DE LA PARADA ---
    @JsonProperty("id")
    private String idParada;
    @JsonProperty("descripcion")
    private String descripcionParada;
    @JsonProperty("descripcion_larga")
    private String descripcionLarga;
    private Double lat;
    private Double lng;

    // --- DATOS DE LA LLEGADA / LÍNEA ---
    private String linea;
    private String descripcionLinea;
    private String destino;
    private Integer minutos;
}