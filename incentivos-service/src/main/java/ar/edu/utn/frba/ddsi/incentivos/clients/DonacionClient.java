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
                    donacionesUrl + idUsuario + "/medios-contacto",
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
}