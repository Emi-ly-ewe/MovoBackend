package org.movo.movobackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroItinerarioResponse {
    private boolean success;
    private List<MetroParada> paradas;
}
