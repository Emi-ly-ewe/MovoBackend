package org.movo.movobackend.controller;

import org.movo.movobackend.model.MetroItinerarioResponse;
import org.movo.movobackend.model.MetroLlegadaResponse;
import org.movo.movobackend.service.MetroService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metro")
public class MetroController {
    private final MetroService metroService;
    public MetroController(MetroService metroService) {this.metroService = metroService;}

    @GetMapping("/itinerario")
    public MetroItinerarioResponse getItinerario(
            @RequestParam(name = "route") int linea,
            @RequestParam(name = "direction") int direccion) {return metroService.obtenerItinerario(linea, direccion);}
    @GetMapping("/llegadas")
    public MetroLlegadaResponse getLlegadas(@RequestParam(name = "stopSAE") int stopSAE) {return metroService.obtenerLlegadas(stopSAE);}
}

