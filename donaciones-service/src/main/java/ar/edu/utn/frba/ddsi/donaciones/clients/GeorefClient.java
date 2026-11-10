package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.georef.GeorefDTOs;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente HTTP de Georef Argentina.
 *
 * <p>Usa el {@link RestTemplate} compartido, que ya tiene timeouts de conexión y lectura
 * (Punto 49). No conoce reglas de negocio: solo manda consultas y devuelve los resultados.
 * Un fallo de infraestructura se relanza como {@link GeorefException} para que la capa de
 * servicio pueda distinguirlo de una dirección sin resultado.
 */
@Service
public class GeorefClient {

    private static final Logger log = LoggerFactory.getLogger(GeorefClient.class);

    private static final String RUTA_DIRECCIONES = "/direcciones";

    private final RestTemplate restTemplate;
    private final String georefUrl;

    public GeorefClient(
            RestTemplate restTemplate,
            @Value("${servicio.georef.url:https://apis.datos.gob.ar/georef/api}") String georefUrl) {
        this.restTemplate = restTemplate;
        this.georefUrl = georefUrl;
    }

    /**
     * Normaliza y geocodifica un lote de direcciones.
     *
     * @return un resultado por consulta, en el mismo orden. Puede venir con
     *         {@code direcciones} vacío si Georef no encontró la dirección.
     * @throws GeorefException si la API no está disponible o responde un error HTTP.
     */
    public List<GeorefDTOs.Resultado> buscar(List<GeorefDTOs.Consulta> consultas) {
        if (consultas == null || consultas.isEmpty()) {
            return List.of();
        }

        String url = url(RUTA_DIRECCIONES);
        try {
            GeorefDTOs.Respuesta respuesta = restTemplate.postForObject(
                    url,
                    new GeorefDTOs.Lote(consultas),
                    GeorefDTOs.Respuesta.class);

            if (respuesta == null || respuesta.resultados() == null) {
                return List.of();
            }
            return respuesta.resultados();
        } catch (RestClientException e) {
            log.warn("Georef no respondió en {} para {} direcciones", url, consultas.size(), e);
            throw new GeorefException("No se pudo consultar Georef", e);
        } catch (RuntimeException e) {
            log.warn("Respuesta inválida de Georef en {} para {} direcciones",
                    url, consultas.size(), e);
            throw new GeorefException("Respuesta inválida de Georef", e);
        }
    }

    private String url(String ruta) {
        return georefUrl.replaceAll("/+$", "") + ruta;
    }
}
