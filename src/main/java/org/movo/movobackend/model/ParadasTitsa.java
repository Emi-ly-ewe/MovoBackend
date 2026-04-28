package org.movo.movobackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ParadasTitsa {
    private String id;
    private String descripcion;
    @JsonProperty("descripcion_larga")
    private String descripcionLarga;
    private String lat;
    private String lng;
}