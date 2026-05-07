package org.movo.movobackend.service;

import org.movo.movobackend.model.*;
import org.movo.movobackend.model.dto.MetroStopApiDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetroService {
    private final RestTemplate restTemplate;
    private static final String urlPanel = "https://tranviaonline.metrotenerife.com/api/infoStops/infoPanel";
    private static final String urlStops = "https://tranviaonline.metrotenerife.com/api/stops";

    public MetroService(RestTemplate restTemplate) {this.restTemplate = restTemplate;}

    private MetroInfoPanel[] descargarPanelCompleto() {
        MetroInfoPanel[] panel = restTemplate.getForObject(urlPanel, MetroInfoPanel[].class);
        return panel != null ? panel : new MetroInfoPanel[0];
    }

    private MetroStopApiDTO[] descargarParadas() {
        MetroStopApiDTO[] paradas = restTemplate.getForObject(urlStops, MetroStopApiDTO[].class);
        return paradas != null ? paradas : new MetroStopApiDTO[0];
    }

    public MetroItinerarioResponse obtenerItinerario(int route, int direction) {
        MetroInfoPanel[] panel = descargarPanelCompleto();
        MetroStopApiDTO[] paradasApi = descargarParadas();

        Map<String, MetroStopApiDTO> mapaCoordenadas = Arrays.stream(paradasApi)
                .filter(stop -> stop.getName() != null)
                .collect(Collectors.toMap(
                        stop -> stop.getName().trim().toLowerCase(),
                        stop -> stop,
                        (a, b) -> a
                ));

        List<MetroParada> paradas = Arrays.stream(panel)
                .filter(p ->
                        Objects.equals(p.getRoute(), route) &&
                                Objects.equals(p.getDirection(), direction)
                )

                .collect(Collectors.toMap(
                        p -> p.getStopSAE() + "-" + p.getOrderStop(),
                        p -> p,
                        (a, b) -> a
                ))
                .values().stream()
                .sorted(Comparator.comparingInt(MetroInfoPanel::getOrderStop))
                .map(p -> {
                    String nombreBuscar = p.getStopDescription() != null ?
                            p.getStopDescription().trim().toLowerCase() : "";

                    MetroStopApiDTO coordenadas = mapaCoordenadas.get(nombreBuscar);

                    return new MetroParada(
                            p.getStopSAE(),
                            p.getStopDescription(),
                            p.getOrderStop(),
                            p.getDirection(),
                            coordenadas != null ? coordenadas.getLat() : null,
                            coordenadas != null ? coordenadas.getLon() : null
                    );
                })
                .toList();

        return new MetroItinerarioResponse(true, paradas);
    }

    public MetroLlegadaResponse obtenerLlegadas(int stopSAE) {
        MetroInfoPanel[] panel = descargarPanelCompleto();
        List<MetroLlegada> llegadas = Arrays.stream(panel)
                .filter(p -> p.getStopSAE() == stopSAE)
                .collect(Collectors.toMap(
                        p -> p.getRoute() + "-" + p.getService(),
                        p -> p,
                        (a, b) -> a
                ))
                .values().stream()
                .sorted(Comparator.comparingInt(MetroInfoPanel::getRemainingMinutes))
                .map(p -> new MetroLlegada(
                        p.getRoute(),
                        p.getDestinationStopDescription(),
                        p.getRemainingMinutes()
                ))
                .toList();
        return new MetroLlegadaResponse(true, llegadas);
    }
}