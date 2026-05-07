package org.movo.movobackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.movo.movobackend.model.*;
import org.movo.movobackend.model.dto.TitsaInfoParadaApiDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TitsaService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Guardamos el último ID en memoria para saber si hay uno nuevo
    private int ultimoIdNotificacionConocido = 6899;

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

    // --- NUEVO: Extracción de Notificación por ID ---
    public TitsaNotificacion getNotificacion(int id) {
        try {
            String url = "https://www.titsa.com/index.php/tus-guaguas/ultima-hora?notification_id=" + id;

            // 1. Dejamos que Jsoup descargue y parsee el UTF-8 automáticamente
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            // 2. Extraemos los textos
            String titulo = doc.select(".item_header").text();
            String contenido = doc.select(".item-content").text();

            // 3. Limpieza de basura (los contadores ocultos de Joomla "0 0 0")
            if (contenido.endsWith("0 0 0")) {
                // Borramos los últimos 5 caracteres y quitamos espacios en blanco
                contenido = contenido.substring(0, contenido.length() - 5).trim();
            }

            return new TitsaNotificacion(String.valueOf(id), titulo, contenido);
        } catch (Exception e) {
            throw new RuntimeException("Error extrayendo la notificación " + id, e);
        }
    }

    // --- NUEVO: Tarea Programada que vigila la web ---
    @Scheduled(fixedRate = 300000) // Se ejecuta cada 5 minutos (300.000 ms)
    public void monitorearNuevasNotificaciones() {
        try {
            String url = "https://www.titsa.com/index.php/tus-guaguas/ultima-hora";
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            // Busca el primer enlace <a> que contenga 'notification_id=' en su href
            Element primerAviso = doc.selectFirst("a[href*=notification_id=]");

            if (primerAviso != null) {
                String href = primerAviso.attr("href");
                String idStr = href.substring(href.indexOf("notification_id=") + 16);
                int idReciente = Integer.parseInt(idStr);

                if (idReciente > ultimoIdNotificacionConocido) {
                    System.out.println("🚨 ¡NUEVO AVISO DE TITSA! ID: " + idReciente);
                    ultimoIdNotificacionConocido = idReciente;

                    // Opcional: Extraer la info al instante y enviarla por WebSocket o Push a los móviles
                    // TitsaNotificacion nuevaAlerta = getNotificacion(idReciente);
                    // notificacionPushService.enviar(nuevaAlerta);
                }
            }
        } catch (Exception e) {
            System.err.println("Error monitoreando TITSA: " + e.getMessage());
        }
    }

    public TitsaLlegadaResponse getLlegadas(int idParada) {
        try {
            String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=" + idParada;

            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

            byte[] rawBytes = response.getBody();

            if (rawBytes == null) {
                throw new RuntimeException("TITSA API returned empty body");
            }

            String rawJson = new String(rawBytes, StandardCharsets.UTF_8);

            TitsaInfoParadaApiDTO apiResponse =
                    objectMapper.readValue(rawJson, TitsaInfoParadaApiDTO.class);

            if (apiResponse == null || !apiResponse.isSuccess()) {
                return new TitsaLlegadaResponse(false, null, List.of());
            }

            TitsaParada parada = apiResponse.getParada();

            if (parada != null) {
                parada.setDescripcion(reverseMojibake(parada.getDescripcion()));
                parada.setDescripcionLarga(
                        reverseMojibake(parada.getDescripcionLarga())
                );
            }

            List<TitsaLlegada> llegadas =
                    apiResponse.getLineas()
                            .stream()
                            .map(linea -> new TitsaLlegada(
                                    linea.getId(),
                                    reverseMojibake(linea.getDestino()),
                                    parseMinutos(linea.getTiempo())
                            ))
                            .toList();

            return new TitsaLlegadaResponse(
                    true,
                    parada,
                    llegadas
            );

        } catch (Exception e) {
            throw new RuntimeException("Error fetching TITSA arrivals", e);
        }
    }

    private String reverseMojibake(String brokenText) {
        if (brokenText == null || brokenText.isEmpty()) {return brokenText;}
        byte[] bytes = brokenText.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private int parseMinutos(String tiempo) {
        if (tiempo == null) {
            return -1;
        }

        tiempo = tiempo.trim();

        try {
            return Integer.parseInt(tiempo);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}