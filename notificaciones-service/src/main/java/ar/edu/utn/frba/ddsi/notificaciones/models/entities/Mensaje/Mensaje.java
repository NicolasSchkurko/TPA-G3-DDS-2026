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
    @Column(name = "cuerpo", nullable = false)
    private String cuerpo;

    public Mensaje(String asunto, String cuerpo) {
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }
}
