package ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mensajes")
@NoArgsConstructor
public class Mensaje {
    @Id
    private UUID id_mensaje =UUID.randomUUID();
    @Getter
    @Column(name = "asunto", nullable = false)
    private String asunto;
    @Getter
    @Column(name = "cuerpo", nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    /** Opcional: link estructurado a un mapa/adjunto; no va pegado en el cuerpo. */
    @Getter
    @Column(name = "url_mapa", length = 1024)
    private String urlMapa;

    public Mensaje(String asunto, String cuerpo) {
        this(asunto, cuerpo, null);
    }

    public Mensaje(String asunto, String cuerpo, String urlMapa) {
        this.asunto = asunto;
        this.cuerpo = cuerpo;
        this.urlMapa = urlMapa;
    }
}
