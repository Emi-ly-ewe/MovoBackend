package org.movo.movobackend.model;

import lombok.Data;
import java.util.List;

@Data
public class TitsaItinerarioResponse {
    private boolean success;
    private List<TitsaItinerario> paradas;
}
