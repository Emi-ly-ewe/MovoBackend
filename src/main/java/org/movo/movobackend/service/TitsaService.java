package org.movo.movobackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.movo.movobackend.model.TitsaItinerarioResponse;
import org.movo.movobackend.model.TitsaParada;
import org.movo.movobackend.model.TitsaParadaResponse;
import org.springframework.http.ResponseEntity;
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

            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] rawBytes = response.getBody();

            if (rawBytes == null) {throw new RuntimeException("TITSA API returned an empty body");}
            String rawJson = new String(rawBytes, StandardCharsets.UTF_8);
            TitsaParadaResponse titsaResponse = objectMapper.readValue(rawJson, TitsaParadaResponse.class);

            if (titsaResponse != null && titsaResponse.getParada() != null) {
                TitsaParada parada = titsaResponse.getParada();

                parada.setDescripcion(reverseMojibake(parada.getDescripcion()));
                parada.setDescripcionLarga(reverseMojibake(parada.getDescripcionLarga()));
            }

            return titsaResponse;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching or parsing TITSA data", e);
        }
    }

    public TitsaItinerarioResponse getItinerario(int linea, int trayecto) {
        try {
            String url = "https://www.titsa.com/ajax/xItinerario.php?c=1234&id_linea=" + linea + "&id_trayecto=" + trayecto;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            String json = new String(response.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, TitsaItinerarioResponse.class);
        } catch (Exception e) {throw new RuntimeException(e);}
    }

    private String reverseMojibake(String brokenText) {
        if (brokenText == null || brokenText.isEmpty()) {return brokenText;}
        byte[] bytes = brokenText.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}