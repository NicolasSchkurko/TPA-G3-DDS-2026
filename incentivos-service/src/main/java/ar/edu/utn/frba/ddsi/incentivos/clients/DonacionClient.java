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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Cliente de {@code donaciones-service}, de donde salen dos cosas: el medio de contacto
 * del donante y la verificación de que quien opera es un administrador.
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
     * El medio de contacto del donante, o {@code null} si no tiene ninguno o si el
     * servicio no respondió.
     *
     * <p>Devolver {@code null} y no propagar la excepción es a propósito: el contacto solo
     * se usa para notificar, y que un donante sin contacto o con el servicio caído no
     * pueda recibir su notificación no puede ser motivo para rechazar la donación.
     *
     * <p>El error se registra con stack trace. Antes se imprimía solo {@code getMessage()}
     * a {@code System.err}, así que cuando una integración fallaba no quedaba rastro de
     * dónde había vindo el problema (punto 23).
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

    /**
     * Si el id corresponde a un administrador.
     *
     * <p>Un 404 (o cualquier 4xx) se distingue del resto de los fallos a propósito: en el
     * primer caso el id simplemente no es de un admin y la respuesta al cliente es un 403
     * limpio; en el segundo el servicio de donaciones está caído, y eso deserves un aviso
     * con stack trace porque es un problema de infraestructura y no del pedido.
     */
    public boolean verificarAdmin(UUID idAdmin) {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                urlDonaciones("/api/admins/" + idAdmin),
                Object.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            log.warn("El id {} no corresponde a un administrador: {}", idAdmin, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("No se pudo verificar el administrador {}", idAdmin, e);
            return false;
        }
    }

    private String urlDonaciones(String path) {
        return donacionesUrl.replaceAll("/+$", "") + path;
    }
}
