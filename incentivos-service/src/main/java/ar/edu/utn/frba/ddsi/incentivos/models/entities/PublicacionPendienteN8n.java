package ar.edu.utn.frba.ddsi.incentivos.models.entities;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Publicacion para n8n guardada en la misma transaccion que otorga la insignia.
 *
 * <p>El lease permite que una instancia reclame la publicacion antes de hacer la llamada
 * HTTP y que otra instancia la recupere si la primera cae durante el envio.
 */
@Getter
@Entity
@Table(name = "publicacion_pendiente_n8n", indexes = {
    @Index(name = "idx_publicacion_n8n_reintento", columnList = "proximo_intento, lease_hasta")
})
@NoArgsConstructor
public class PublicacionPendienteN8n {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(nullable = false, length = 100)
    private String redSocial;

    @Column(nullable = false)
    private String nombreUsuario;

    @Column(nullable = false)
    private UUID idUsuario;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(nullable = false)
    private LocalDateTime proximoIntento;

    private LocalDateTime leaseHasta;

    @Column(nullable = false)
    private int intentos;

    @Column(length = 2000)
    private String ultimoError;

    public PublicacionPendienteN8n(PerfilPublicacionDTO publicacion, LocalDateTime ahora) {
        this.prompt = publicacion.getPrompt();
        this.mensaje = publicacion.getMensaje();
        this.redSocial = publicacion.getRedSocial();
        this.nombreUsuario = publicacion.getNomUsuario();
        this.idUsuario = publicacion.getIdUsuario();
        this.fechaCreacion = ahora;
        this.proximoIntento = ahora;
    }

    public PerfilPublicacionDTO comoDto() {
        return new PerfilPublicacionDTO(prompt, mensaje, redSocial, nombreUsuario, idUsuario);
    }

    public void reclamarHasta(LocalDateTime vencimiento) {
        this.leaseHasta = vencimiento;
        this.intentos++;
    }

    public void reintentarEn(LocalDateTime proximoIntento, String error) {
        this.proximoIntento = proximoIntento;
        this.leaseHasta = null;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 2000));
    }
}
