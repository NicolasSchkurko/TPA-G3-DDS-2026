package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class DonacionClient {
    @Value("${servicio.donaciones.url}")
    private String donacionesUrl;

    private final RestTemplate restTemplate;

    public DonacionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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
            System.err.println("No se pudo obtener el contacto de la persona: " + e.getMessage());
            return null;
        }
    }

    public boolean verificarAdmin(UUID idAdmin) {
        try {
            // Consultamos al microservicio de Donaciones al endpoint correcto: /api/admins/{id}
            org.springframework.http.ResponseEntity<Object> response = restTemplate.getForEntity(
                urlDonaciones("/api/admins/" + idAdmin),
                Object.class
            );

            // Si devuelve un código 2xx (ej. 200 OK), el admin existe
            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException e) {
            // Captura errores 4xx (como 404 Not Found si el ID no corresponde a un admin)
            System.err.println("El administrador no fue encontrado o la petición es inválida: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Captura cualquier otro error de conexión o del servidor (5xx)
            System.err.println("Error de conexión al verificar el rol de administrador: " + e.getMessage());
            return false;
        }
    }

    private String urlDonaciones(String path) {
        return donacionesUrl.replaceAll("/+$", "") + path;
    }
}
