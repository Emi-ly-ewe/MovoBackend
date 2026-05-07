package org.movo.movobackend.model.response;

import lombok.Data;
import org.movo.movobackend.model.TitsaItinerario;

import java.util.List;

@Data
public class TitsaItinerarioResponse {
    private boolean success;
    private List<TitsaItinerario> paradas;
}
