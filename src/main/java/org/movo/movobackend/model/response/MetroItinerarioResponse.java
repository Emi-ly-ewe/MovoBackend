package org.movo.movobackend.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.movo.movobackend.model.MetroParada;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroItinerarioResponse {
    private boolean success;
    private List<MetroParada> paradas;
}
