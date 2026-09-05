package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.MedioContactoDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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
            MedioContactoDTO dto = restTemplate.getForObject(
                donacionesUrl + "api/donante/" + idUsuario + "/medios-contacto",
                MedioContactoDTO.class
            );

            if (dto == null) {
                return null;
            }

            return new MedioContacto(
                dto.getMedioDeContacto(),
                dto.getDireccionContacto()
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
                donacionesUrl + "api/admins/" + idAdmin,
                Object.class
            );

            // Si devuelve un código 2xx (ej. 200 OK), el admin existe
            return response.getStatusCode().is2xxSuccessful();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Captura errores 4xx (como 404 Not Found si el ID no corresponde a un admin)
            System.err.println("El administrador no fue encontrado o la petición es inválida: " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Captura cualquier otro error de conexión o del servidor (5xx)
            System.err.println("Error de conexión al verificar el rol de administrador: " + e.getMessage());
            return false;
        }
    }
}