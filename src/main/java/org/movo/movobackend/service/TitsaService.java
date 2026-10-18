package org.movo.movobackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TitsaService {
    private static final Logger log = LoggerFactory.getLogger(TitsaService.class);

    // Max number of nearby stops we ask TITSA for arrivals (each one is an upstream call on a cache miss)
    private static final int MAX_PARADAS_CERCANAS = 10;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LineaRepository lineaRepository;
    private final ParadaRepository paradaRepository;

    // --- CACHES ---
    // Arrivals: short TTL. Caffeine also makes concurrent requests for the same stop share ONE upstream call.
    private final Cache<Integer, TitsaParadaResponse> llegadasCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(5_000)
            .build();
    // Stop info and itineraries barely change
    private final Cache<Integer, TitsaParadaResponse> paradaCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(5_000)
            .build();
    private final Cache<String, TitsaItinerarioResponse> itinerarioCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(500)
            .build();
    private final Cache<Integer, TitsaNotificacion> notificacionCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1_000)
            .build();
    // The deduplicated list of stops (avoids findAll() on every /cercanas request)
    private final Cache<String, List<ParadaEntity>> paradasUnicasCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(1)
            .build();

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

    private List<ParadaEntity> paradasUnicas() {
        return paradasUnicasCache.get("all", k -> List.copyOf(
                paradaRepository.findAll().stream()
                        .collect(Collectors.toMap(ParadaEntity::getParadaId, p -> p, (p1, p2) -> p1))
                        .values()));
    }

    public List<TitsaParadaResponse> getParadasCercanas(double latUsuario, double lngUsuario, int radioMetros) {
        record Cerca(ParadaEntity parada, double metros) {}

        // Nearest first, capped, so one request can't fan out into dozens of upstream calls
        List<ParadaEntity> cercanas = paradasUnicas().stream()
                .filter(p -> p.getLat() != null && p.getLng() != null)
                .map(p -> new Cerca(p, calcularDistancia(latUsuario, lngUsuario, p.getLat(), p.getLng())))
                .filter(c -> c.metros() <= radioMetros)
                .sorted(Comparator.comparingDouble(Cerca::metros))
                .limit(MAX_PARADAS_CERCANAS)
                .map(Cerca::parada)
                .toList();

        return cercanas.stream()
                .map(entidadParada -> {
                    TitsaParadaResponse response = getLlegadas(Integer.parseInt(entidadParada.getParadaId()));

                    // --- VARIANTE COMPLETO O MEDIO (La API funcionó) ---
                    if (response.isSuccess() && response.getParada() != null) {
                        if (response.getLlegadas() == null) {
                            response.setLlegadas(List.of()); // Aseguramos array vacío en vez de null
                        }
                        return response;
                    }

                    // --- VARIANTE MEDIO / FALLBACK (La API falló o no tiene datos) ---
                    TitsaParada paradaFallback = new TitsaParada();
                    paradaFallback.setIdParada(entidadParada.getParadaId());
                    paradaFallback.setLat(entidadParada.getLat());
                    paradaFallback.setLng(entidadParada.getLng());

                    return new TitsaParadaResponse(false, paradaFallback, List.of()); // Devuelve llegadas vacías []
                })
                .toList(); // Si no hay paradas en el radio, esto suelta el Variante Vacío: []
    }

    // --- MÉTODOS DE PARADAS Y LLEGADAS ---
    public TitsaParadaResponse getParada(int id) {
        return paradaCache.get(id, this::fetchParada);
    }

    private TitsaParadaResponse fetchParada(int id) {
        try {
            String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=" + id;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] rawBytes = response.getBody();

            if (rawBytes == null) { throw new RuntimeException("TITSA API returned an empty body"); }

            String rawJson = new String(rawBytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(rawJson);

            // TITSA answers "parada": false for unknown stops / no data
            if (!root.path("parada").isObject()) {
                return new TitsaParadaResponse(false, null, List.of());
            }

            TitsaParadaResponse titsaResponse = objectMapper.treeToValue(root, TitsaParadaResponse.class);

            if (titsaResponse != null && titsaResponse.getParada() != null) {
                TitsaParada parada = titsaResponse.getParada();
                parada.setDescripcionParada(reverseMojibake(parada.getDescripcionParada()));
                parada.setDescripcionLarga(reverseMojibake(parada.getDescripcionLarga()));
            }

            return titsaResponse;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching or parsing TITSA data for stop " + id, e);
        }
    }

    public TitsaParadaResponse getLlegadas(int idParada) {
        return llegadasCache.get(idParada, this::fetchLlegadas);
    }

    private TitsaParadaResponse fetchLlegadas(int idParada) {
        try {
            String url = "https://www.titsa.com/ajax/xGetInfoParada.php?id_parada=" + idParada;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            byte[] rawBytes = response.getBody();

            if (rawBytes == null) { throw new RuntimeException("TITSA API returned empty body"); }

            String rawJson = new String(rawBytes, StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(rawJson);

            // TITSA answers {"success":true,"parada":false,"lineas":null} for unknown stops / no data.
            // That is a valid "no arrivals" answer, not an error.
            if (!root.path("success").asBoolean(false) || !root.path("parada").isObject()) {
                return new TitsaParadaResponse(false, null, List.of());
            }

            TitsaInfoParadaApiDTO apiResponse = objectMapper.treeToValue(root, TitsaInfoParadaApiDTO.class);

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
            // One short line instead of a stack trace. The empty answer is cached for a few seconds,
            // which also stops us hammering TITSA while it is failing.
            log.warn("TITSA arrivals failed for stop {}: {}", idParada, e.toString());
            return new TitsaParadaResponse(false, null, List.of());
        }
    }

    public TitsaItinerarioResponse getItinerario(int linea, int trayecto) {
        return itinerarioCache.get(linea + "-" + trayecto, k -> fetchItinerario(linea, trayecto));
    }

    private TitsaItinerarioResponse fetchItinerario(int linea, int trayecto) {
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
        return notificacionCache.get(id, this::fetchNotificacion);
    }

    private TitsaNotificacion fetchNotificacion(int id) {
        try {
            String url = "https://www.titsa.com/index.php/tus-guaguas/ultima-hora?notification_id=" + id;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(5000)
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
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).get();
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
