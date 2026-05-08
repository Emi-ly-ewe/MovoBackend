package org.movo.movobackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.movo.movobackend.model.*;
import org.movo.movobackend.model.dto.TitsaInfoParadaApiDTO;
import org.movo.movobackend.model.entity.LineaEntity;
import org.movo.movobackend.model.entity.ParadaEntity;
import org.movo.movobackend.model.response.TitsaItinerarioResponse;
import org.movo.movobackend.model.response.TitsaParadaResponse;
import org.movo.movobackend.repository.LineaRepository;
import org.movo.movobackend.repository.ParadaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TitsaService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LineaRepository lineaRepository;
    private final ParadaRepository paradaRepository;

    private int ultimoIdNotificacionConocido = 6899;

    public TitsaService(RestTemplate restTemplate, ObjectMapper objectMapper, LineaRepository lineaRepository,ParadaRepository paradaRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.lineaRepository = lineaRepository;
        this.paradaRepository = paradaRepository;
    }

    // --- MÉTODOS DE LÍNEAS (Base de Datos) ---
    public List<TitsaLinea> getTodasLasLineasTitsa() {
        List<LineaEntity> lineasBD = lineaRepository.findByOperador("TITSA");
        return lineasBD.stream()
                .map(entidad -> new TitsaLinea(entidad.getId(), entidad.getNombre()))
                .collect(Collectors.toList());
    }
    public List<TitsaParada> getParadasPorLinea(String linea, int sentido) {
        List<ParadaEntity> paradasBD = paradaRepository.findByLineaAndSentidoOrderByOrdenParadaAsc(linea, sentido);

        return paradasBD.stream().map(entidad -> {
            TitsaParada p = new TitsaParada();
            p.setIdParada(entidad.getParadaId());
            p.setDescripcionParada(entidad.getNombre());
            p.setDescripcionLarga(entidad.getNombre());
            p.setLat(entidad.getLat());
            p.setLng(entidad.getLng());
            return p;
        }).toList();
    }

    public List<TitsaParada> getParadasCercanas(double latUsuario, double lngUsuario, int radioMetros) {
        List<ParadaEntity> todasLasParadas = paradaRepository.findAll();

        return todasLasParadas.stream()
                .filter(p -> calcularDistancia(latUsuario, lngUsuario, p.getLat(), p.getLng()) <= radioMetros).map(ParadaEntity::getParadaId)
                .distinct().flatMap(idParada -> {
                    TitsaParadaResponse response = getLlegadas(Integer.parseInt(idParada));

                    if (response.isSuccess() && response.getLlegadas() != null && !response.getLlegadas().isEmpty()) {

                        return response.getLlegadas().stream().map(llegada -> {
                            TitsaParada p = new TitsaParada();
                            p.setIdParada(response.getParada().getIdParada());
                            p.setDescripcionParada(response.getParada().getDescripcionParada());
                            p.setDescripcionLarga(response.getParada().getDescripcionLarga());
                            p.setLat(response.getParada().getLat());
                            p.setLng(response.getParada().getLng());

                            p.setLinea(llegada.getLinea());
                            p.setDescripcionLinea(llegada.getDescripcionLinea()); // Lo nuevo que vimos en el JSON
                            p.setDestino(llegada.getDestino());
                            p.setMinutos(llegada.getMinutos());

                            return p;
                        });
                    }

                    // (Opcional) Fallback: Si la API no tiene llegadas, devolvemos la parada sin info de líneas
                    TitsaParada pVacia = new TitsaParada();
                    pVacia.setIdParada(idParada);
                    pVacia.setMinutos(-1); // Para saber que no hay datos
                    return java.util.stream.Stream.of(pVacia);
                })
                .toList();
    }

    // --- MÉTODOS DE PARADAS Y LLEGADAS ---
    public TitsaParadaResponse getParada(int id) {
        try {
            String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=" + id;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] rawBytes = response.getBody();

            if (rawBytes == null) { throw new RuntimeException("TITSA API returned an empty body"); }

            String rawJson = new String(rawBytes, StandardCharsets.UTF_8);
            TitsaParadaResponse titsaResponse = objectMapper.readValue(rawJson, TitsaParadaResponse.class);

            if (titsaResponse != null && titsaResponse.getParada() != null) {
                TitsaParada parada = titsaResponse.getParada();
                parada.setDescripcionParada(reverseMojibake(parada.getDescripcionParada()));
                parada.setDescripcionLarga(reverseMojibake(parada.getDescripcionLarga()));
            }

            return titsaResponse;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching or parsing TITSA data", e);
        }
    }

    public TitsaParadaResponse getLlegadas(int idParada) {
        try {
            String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=" + idParada;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] rawBytes = response.getBody();

            if (rawBytes == null) { throw new RuntimeException("TITSA API returned empty body"); }

            String rawJson = new String(rawBytes, StandardCharsets.UTF_8);
            TitsaInfoParadaApiDTO apiResponse = objectMapper.readValue(rawJson, TitsaInfoParadaApiDTO.class);

            if (apiResponse == null || !apiResponse.isSuccess()) {
                return new TitsaParadaResponse(false, null, List.of());
            }

            TitsaParada parada = apiResponse.getParada();
            if (parada != null) {
                parada.setDescripcionParada(reverseMojibake(parada.getDescripcionParada()));
                parada.setDescripcionLarga(reverseMojibake(parada.getDescripcionLarga()));
            }

            List<TitsaParada> llegadas = List.of();

            if (apiResponse.getLineas() != null) {
                llegadas = apiResponse.getLineas().stream()
                        .map(linea -> {
                            TitsaParada p = new TitsaParada();
                            p.setLinea(linea.getId());
                            p.setDescripcionLinea(reverseMojibake(linea.getDescripcion()));
                            p.setDestino(reverseMojibake(linea.getDestino()));
                            p.setMinutos(parseMinutos(linea.getTiempo()));
                            return p;
                        })
                        .toList();
            }

            return new TitsaParadaResponse(true, parada, llegadas);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching TITSA arrivals", e);
        }
    }

    public TitsaItinerarioResponse getItinerario(int linea, int trayecto) {
        try {
            String url = "https://www.titsa.com/ajax/xItinerario.php?c=1234&id_linea=" + linea + "&id_trayecto=" + trayecto;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            String json = new String(response.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, TitsaItinerarioResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- MÉTODOS DE NOTIFICACIONES (Scraping) ---
    public TitsaNotificacion getNotificacion(int id) {
        try {
            String url = "https://www.titsa.com/index.php/tus-guaguas/ultima-hora?notification_id=" + id;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();

            String titulo = doc.select(".item_header").text();
            String contenido = doc.select(".item-content").text();

            if (contenido.endsWith("0 0 0")) {
                contenido = contenido.substring(0, contenido.length() - 5).trim();
            }

            return new TitsaNotificacion(String.valueOf(id), titulo, contenido);
        } catch (Exception e) {
            throw new RuntimeException("Error extrayendo la notificación " + id, e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void monitorearNuevasNotificaciones() {
        try {
            String url = "https://www.titsa.com/index.php/tus-guaguas/ultima-hora";
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").get();
            Element primerAviso = doc.selectFirst("a[href*=notification_id=]");

            if (primerAviso != null) {
                String href = primerAviso.attr("href");
                String idStr = href.substring(href.indexOf("notification_id=") + 16);
                int idReciente = Integer.parseInt(idStr);

                if (idReciente > ultimoIdNotificacionConocido) {
                    System.out.println("🚨 ¡NUEVO AVISO DE TITSA! ID: " + idReciente);
                    ultimoIdNotificacionConocido = idReciente;
                }
            }
        } catch (Exception e) {
            System.err.println("Error monitoreando TITSA: " + e.getMessage());
        }
    }

    // --- UTILIDADES ---
    private String reverseMojibake(String brokenText) {
        if (brokenText == null || brokenText.isEmpty()) { return brokenText; }
        byte[] bytes = brokenText.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private int parseMinutos(String tiempo) {
        if (tiempo == null) return -1;
        tiempo = tiempo.trim();
        try {
            return Integer.parseInt(tiempo);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        // Radio de la Tierra en metros
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Resultado en metros
    }
}