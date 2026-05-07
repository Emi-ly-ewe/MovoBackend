package org.movo.movobackend.controller;

import org.movo.movobackend.model.TitsaItinerarioResponse;
import org.movo.movobackend.model.TitsaLlegadaResponse;
import org.movo.movobackend.model.TitsaNotificacion;
import org.movo.movobackend.model.TitsaParadaResponse;
import org.movo.movobackend.service.TitsaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/titsa")
public class TitsaController {
    private final TitsaService titsaService;

    public TitsaController(TitsaService titsaService) {this.titsaService = titsaService;}

    @GetMapping("/parada/{id}")
    public TitsaParadaResponse getParada(@PathVariable int id) {return titsaService.getParada(id);}
    @GetMapping("/itinerario")
    public TitsaItinerarioResponse getItinerario(@RequestParam int linea, @RequestParam int trayecto) {return titsaService.getItinerario(linea, trayecto);}
    @GetMapping("/notificacion/{id}")
    public TitsaNotificacion getNotificacion(@PathVariable int id) {return titsaService.getNotificacion(id);}
    @GetMapping("/llegadas/{id}")
    public TitsaLlegadaResponse getLlegadas(@PathVariable int id) {
        return titsaService.getLlegadas(id);
    }
}