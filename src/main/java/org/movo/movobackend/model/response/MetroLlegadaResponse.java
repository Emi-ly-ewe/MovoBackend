package org.movo.movobackend.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.movo.movobackend.model.MetroLlegada;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetroLlegadaResponse {
    private boolean success;
    private List<MetroLlegada> llegadas;
}
