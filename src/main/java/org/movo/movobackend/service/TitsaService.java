package org.movo.movobackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.movo.movobackend.model.TitsaItinerarioResponse;
import org.movo.movobackend.model.TitsaParadaResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

@Service
public class TitsaService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TitsaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public TitsaParadaResponse getParada(int id) {
        try {
            String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=" + id;
            String respuestaBase = restTemplate.getForObject(url, String.class);
            byte[] bytes = respuestaBase.getBytes(StandardCharsets.ISO_8859_1);
            String respuestaLimpia = new String(bytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(respuestaLimpia, TitsaParadaResponse.class);
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    public TitsaItinerarioResponse getItinerario(int linea, int trayecto) {
        try {
            String url = "https://www.titsa.com/ajax/xItinerario.php?c=1234&id_linea=" + linea + "&id_trayecto=" + trayecto;
            String respuestaBase = restTemplate.getForObject(url, String.class);
            byte[] bytes = respuestaBase.getBytes(StandardCharsets.ISO_8859_1);
            String respuestaLimpia = new String(bytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(respuestaLimpia, TitsaItinerarioResponse.class);
        } catch (Exception e) {throw new RuntimeException(e);}
    }
}