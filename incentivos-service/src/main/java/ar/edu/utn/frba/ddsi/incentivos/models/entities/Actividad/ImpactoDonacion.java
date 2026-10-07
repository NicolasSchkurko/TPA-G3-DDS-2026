package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una donación copiada desde {@code donaciones-service}, y la fila de la que se calcula el
 * progreso. Los dos índices cubren las consultas por donante y por misión.
 *
 * <p>No tiene setters de negocio: los campos que se escriben después de crearla lo hacen con
 * métodos que dicen qué registran.
 */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "impacto_donacion", indexes = {
        @Index(name = "idx_impacto_usuario_fecha", columnList = "id_usuario, fecha_entrega"),
        @Index(name = "idx_impacto_usuario_mision_fecha",
                columnList = "id_usuario, id_mision, fecha_entrega")
})
public class ImpactoDonacion {

    /**
     * Id de la donación de origen: es la primary key, sin {@code @GeneratedValue}, lo que
     * hace idempotente el endpoint. Por eso es obligatorio en el DTO.
     */
    @Id
    @Setter // Lo asigna el servicio de origen.
    private UUID idDonacion;

    private UUID idUsuario; // id de donaciones
    private LocalDateTime fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;

    /**
     * Si esta donación hizo progresar la misión. La constancia cuenta solo las que aportaron
     * al avance para reconstruir la racha.
     */
    private Boolean hizoProgresarMision = false;

    /** La misión que el donante tenía cuando entró esta donación, aunque no haya aportado. */
    private UUID idMision;

    /** Si esta donación completó la misión, para repetir la misma respuesta ante un reintento. */
    private Boolean completMision = false;

    public ImpactoDonacion(UUID idDonacion,
                           UUID idUsuario,
                           String entidadBeneficiaria,
                           Integer cantidadBienes,
                           LocalDateTime fechaEntrega,
                           String categoria,
                           String subCategoria,
                           String estado) {
        this.idDonacion = idDonacion;
        this.idUsuario = idUsuario;
        this.entidadBeneficiaria = entidadBeneficiaria;
        this.cantidadBienes = cantidadBienes;
        this.fechaEntrega = fechaEntrega;
        this.categoria = categoria;
        this.subCategoria = subCategoria;
        this.estado = estado;
    }

    /** Registra contra qué misión se evaluó esta donación y si aportó al avance. */
    public void registrarProgresoEn(UUID idMision, boolean hizoProgresar) {
        this.idMision = idMision;
        this.hizoProgresarMision = hizoProgresar;
    }

    /** Guarda si esta donación completó la misión, para repetir la respuesta ante un reintento. */
    public void registrarSiCompletoMision(boolean completo) {
        this.completMision = completo;
    }
}
