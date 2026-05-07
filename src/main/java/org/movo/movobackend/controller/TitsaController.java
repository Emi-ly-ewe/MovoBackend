package org.movo.movobackend.controller;

import org.movo.movobackend.model.TitsaLinea;
import org.movo.movobackend.model.TitsaParada;
import org.movo.movobackend.model.response.TitsaItinerarioResponse;
import org.movo.movobackend.model.response.TitsaLlegadaResponse;
import org.movo.movobackend.model.TitsaNotificacion;
import org.movo.movobackend.model.response.TitsaParadaResponse;
import org.movo.movobackend.service.TitsaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping("/lineas")
    public List<TitsaLinea> getLineas() {
        return titsaService.getTodasLasLineasTitsa();
    }
    @GetMapping("/paradas")
    public List<TitsaParada> getParadasDeLinea(@RequestParam String linea, @RequestParam int sentido) {
        return titsaService.getParadasPorLinea(linea, sentido);
    }
    @GetMapping("/cercanas")
    public List<TitsaParada> getCercanas(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "500") int radio) {
        return titsaService.getParadasCercanas(lat, lng, radio);
    }
}