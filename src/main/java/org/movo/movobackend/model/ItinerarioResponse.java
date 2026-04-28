package org.movo.movobackend.model;

import lombok.Data;
import java.util.List;

@Data
public class ItinerarioResponse {
    private boolean success;
    private List<ItinerarioTitsa> paradas;
}
