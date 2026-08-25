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
        MedioContactoDTO dto = restTemplate.getForObject(
                donacionesUrl + idUsuario,
                MedioContactoDTO.class
        );

        return new MedioContacto(
                dto.getMedioDeContacto(),
                dto.getDireccionContacto()
        );
    }
}
