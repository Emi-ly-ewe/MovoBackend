package org.movo.movobackend.controller;

import org.movo.movobackend.model.ItinerarioResponse;
import org.movo.movobackend.model.ParadaTitsaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class PruebaControllerTitsa {
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/test/parada")
    public ParadaTitsaResponse testParada() {
        String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=7043";
        //return restTemplate.getForObject(url, ParadaTitsaResponse.class);
        String response = restTemplate.getForObject(url, String.class);
        System.out.println(response);
        return null;
    }

    @GetMapping("/test/itinerario")
    public ItinerarioResponse testItinerario() {
        String url = "https://www.titsa.com/ajax/xItinerario.php?c=1234&id_linea=122&id_trayecto=12";
        //return restTemplate.getForObject(url, ItinerarioResponse.class);
        String response = restTemplate.getForObject(url, String.class);
        System.out.println(response);
        return null;
    }
}