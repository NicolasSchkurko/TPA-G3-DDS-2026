package ar.edu.utn.frba.ddsi.donaciones.models.Notificador;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

public class Notificador {

    private final RestTemplate restTemplate;

    // @Value("${NOTIFICACIONES_SERVICE_URL}") esto es lo del .env
    private String notificacionesUrl;

    public Notificador(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    // esto no sabemos si esta bien, hay q plantearlo mejor
    public void notificar(Long idDestinatario, String asunto, String cuerpo) {
        Map<String, Object> dto = Map.of(
                "servicioOrigen", "donaciones",
                "idDestinatario", idDestinatario,
                "fecha", LocalDateTime.now().toString(),
                "asuntoMensaje", asunto,
                "cuerpoMensaje", cuerpo
        );

        restTemplate.postForEntity(
                notificacionesUrl + "/notificaciones",
                dto,
                String.class
        );
    }
}
