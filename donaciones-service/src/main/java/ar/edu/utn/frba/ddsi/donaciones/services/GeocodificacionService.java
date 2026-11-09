package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class GeocodificacionService {
    private static final Logger log = LoggerFactory.getLogger(GeocodificacionService.class);
    private static final long MIN_REQUEST_INTERVAL_MS = 1_000;

    private final RestTemplate restTemplate;
    private final String nominatimUrl;
    private final String userAgent;
    private final Map<String, Optional<Coordenadas>> cache = new HashMap<>();
    private long nextRequestAt;

    public GeocodificacionService(
            RestTemplate restTemplate,
            @Value("${geocodificacion.nominatim.url:https://nominatim.openstreetmap.org}") String nominatimUrl,
            @Value("${geocodificacion.nominatim.user-agent:DDSI-TPA-G3-DDSI-2026/1.0 (https://github.com/NicolasSchkurko/TPA-G3-DDSI-2026)}") String userAgent) {
        this.restTemplate = restTemplate;
        this.nominatimUrl = nominatimUrl;
        this.userAgent = userAgent;
    }

    public synchronized Optional<Coordenadas> geocodificar(Direccion direccion) {
        if (direccion == null || direccion.getCalleUno() == null || direccion.getCalleUno().isBlank()
                || direccion.getCiudad() == null || direccion.getCiudad().getNombre() == null
                || direccion.getCiudad().getNombre().isBlank()) {
            return Optional.empty();
        }

        String clave = construirClave(direccion);
        if (cache.containsKey(clave)) {
            return cache.get(clave);
        }

        try {
            esperarIntervalo();
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(nominatimUrl)
                    .pathSegment("search")
                    .queryParam("street", construirCalle(direccion))
                    .queryParam("city", direccion.getCiudad().getNombre())
                    .queryParam("format", "jsonv2")
                    .queryParam("limit", 1);
            if (direccion.getCiudad().getProvincia() != null) {
                uriBuilder.queryParam("state", direccion.getCiudad().getProvincia().getNombre());
            }
            String pais = direccion.getCiudad().getProvincia() == null
                    || direccion.getCiudad().getProvincia().getPais() == null
                    ? "Argentina"
                    : direccion.getCiudad().getProvincia().getPais().getNombre();
            URI uri = uriBuilder.queryParam("country", pais).build().encode().toUri();

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, userAgent);
            ResponseEntity<ResultadoNominatim[]> respuesta = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), ResultadoNominatim[].class);
            ResultadoNominatim[] resultados = respuesta.getBody();

            if (resultados == null || resultados.length == 0) {
                cache.put(clave, Optional.empty());
                return cache.get(clave);
            }
            if (resultados[0].lat() == null || resultados[0].lon() == null) {
                log.warn("Nominatim devolvió un resultado sin coordenadas");
                return Optional.empty();
            }
            double latitud = Double.parseDouble(resultados[0].lat());
            double longitud = Double.parseDouble(resultados[0].lon());
            if (!Double.isFinite(latitud) || !Double.isFinite(longitud)
                    || Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
                log.warn("Nominatim devolvió coordenadas fuera de rango");
                return Optional.empty();
            }
            Optional<Coordenadas> coordenadas = Optional.of(new Coordenadas(latitud, longitud));
            cache.put(clave, coordenadas);
            return coordenadas;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Se interrumpió la espera para consultar Nominatim");
            return Optional.empty();
        } catch (RestClientException | NumberFormatException e) {
            log.warn("No se pudo geocodificar la dirección de la entidad: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized void esperarIntervalo() throws InterruptedException {
        long espera = nextRequestAt - System.currentTimeMillis();
        if (espera > 0) {
            Thread.sleep(espera);
        }
        nextRequestAt = System.currentTimeMillis() + MIN_REQUEST_INTERVAL_MS;
    }

    private static String construirCalle(Direccion direccion) {
        return direccion.getAltura() == null
                ? direccion.getCalleUno()
                : direccion.getCalleUno() + " " + direccion.getAltura();
    }

    private static String construirClave(Direccion direccion) {
        return (construirCalle(direccion) + "|" + direccion.getCiudad().getNombre() + "|"
                + (direccion.getCiudad().getProvincia() == null ? "" : direccion.getCiudad().getProvincia().getNombre())
                + "|" + (direccion.getCiudad().getProvincia() == null
                        || direccion.getCiudad().getProvincia().getPais() == null
                        ? "Argentina" : direccion.getCiudad().getProvincia().getPais().getNombre()))
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record Coordenadas(double latitud, double longitud) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResultadoNominatim(String lat, String lon) {}
}
