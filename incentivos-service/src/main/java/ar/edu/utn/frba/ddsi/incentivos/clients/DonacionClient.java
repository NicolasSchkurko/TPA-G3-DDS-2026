package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente de {@code donaciones-service} para resolver el medio de contacto del donante.
 */
@Slf4j
@Service
public class DonacionClient {

    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    private final RestTemplate restTemplate;

    public DonacionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * El medio de contacto del donante, o {@code null} si no tiene ninguno o si el servicio
     * no respondió. No propaga: el contacto solo se usa para notificar.
     */
    public MedioContacto obtenerContactoPersona(UUID idUsuario) {
        try {
            ResponseEntity<List<Map<String, String>>> response = restTemplate.exchange(
                urlDonaciones("/api/personas/" + idUsuario + "/medios-contacto"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );

            List<Map<String, String>> contactos = response.getBody();
            if (contactos == null || contactos.isEmpty()) {
                return null;
            }

            Map<String, String> dto = contactos.getFirst();
            return new MedioContacto(
                dto.get("tipo"),
                dto.get("valor")
            );
        } catch (Exception e) {
            log.warn("No se pudo obtener el contacto del usuario {}", idUsuario, e);
            return null;
        }
    }

    private String urlDonaciones(String path) {
        return donacionesUrl.replaceAll("/+$", "") + path;
    }
}
