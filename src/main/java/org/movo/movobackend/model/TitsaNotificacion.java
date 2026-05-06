package org.movo.movobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TitsaNotificacion {
    private String id;
    private String titulo;
    private String contenido;
}