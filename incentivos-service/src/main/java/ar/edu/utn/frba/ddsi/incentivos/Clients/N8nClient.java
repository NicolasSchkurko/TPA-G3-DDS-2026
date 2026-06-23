package ar.edu.utn.frba.ddsi.incentivos.Clients;

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioPublicacionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class N8nClient {
    // cliente para consumir n8n y publicar cuando perfil gana una insignia
    @Value("${servicio.n8n.url}")
    private String n8nUrl;

    private final RestTemplate restTemplate;

    public N8nClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void publicarInsignia(PerfilPublicacionDTO dto) throws EnvioPublicacionException {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("usuario", dto.getNombreUsuario());
            body.put("insignia", dto.getNombreInsignia());
            body.put("descripcion", dto.getDescripcionInsignia());

            restTemplate.postForEntity(
                    n8nUrl,
                    body,
                    void.class
            );
        } catch (Exception e) {
            throw new EnvioPublicacionException(dto);
        }
    }
}