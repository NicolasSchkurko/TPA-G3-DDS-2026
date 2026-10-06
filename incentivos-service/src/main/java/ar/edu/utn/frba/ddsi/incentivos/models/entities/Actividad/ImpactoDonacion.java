package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una donación, tal como la reportó {@code donaciones-service}, y el rastro de qué hizo
 * con el avance del donante.
 *
 * <p>No tiene setters de negocio: los tres campos que se escriben después de crearla
 * ({@code idMision}, {@code hizoProgresarMision} y {@code completMision}) se escriben con
 * métodos que dicen qué están registrando. Dejarlos abiertos a {@code setX} desde cualquier
 * lado hacía posible que una fila quedara con datos que nunca pudieron pasar por el
 * agregado.
 */
@Getter
@Entity
@NoArgsConstructor
@Table(name = "impacto_donacion")
public class ImpactoDonacion {

    /**
     * Id de la DONACION, y es el de ORIGEN: se guarda tal cual viene de
     * `donaciones-service`, sin traducirlo ni generar otro.
     *
     * <p>Que sea la primary key y no un autogenerado es lo que hace idempotente el
     * endpoint `PATCH /donacion/{idUsuario}` (punto 14): si la fila ya existe, la
     * peticion es un reintento y se devuelve el resultado guardado sin reprocesar. Con un
     * id autogenerado cada intento insertaba una fila nueva y volvia a aplicar la regla,
     * con lo que el progreso quedaba inflado y se otorgaban insignias antes de tiempo.
     *
     * <p>No lleva `@GeneratedValue` a proposito: el id lo asigna el servicio que
     * origina la donacion. Por eso `idDonacion` es obligatorio en el DTO; si faltara, el
     * alta se rechaza con un 400 en vez de guardar una fila sin clave con la que
     * deduplicar.
     */
    @Id
    @Setter // Lo asigna el servicio de origen. Ver la nota del punto 14 arriba.
    private UUID idDonacion;

    private UUID idUsuario; // id de donaciones
    private LocalDateTime fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;

    /**
     * Indica si esta donación hizo progresar la misión que el donante tenía en el momento
     * en que ingresó. Lo escribe {@code ProgresoMision.evaluarProgreso}.
     *
     * <p>Es lo que después permite reconstruir una racha: la constancia cuenta solo las
     * donaciones que aportaron al avance, así que una donación que no coincidió con la
     * regla no cuenta como mes.
     */
    private Boolean hizoProgresarMision = false;

    /**
     * La misión que el donante tenía cuando entró esta donación. Se guarda aunque la
     * aunque la donación no haya aportado nada, porque es lo que permite saber después
     * qué criterio se estaba evaluando.
     */
    private UUID idMision;

    /**
     * Si esta donacion completo la mision del donante. Se guarda para poder repetir la
     * misma respuesta cuando llega un reintento: un endpoint idempotente no puede devolver
     * un resultado distinto la segunda vez, porque el cliente ya recibio una respuesta y
     * si cambiara lo tomaria por un fallo.
     */
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

    /**
     * Registra contra qué misión se evaluó esta donación y si aportó al avance.
     *
     * <p>Los dos datos van juntos porque se deciden en el mismo momento: la regla se
     * aplica contra una misión concreta, y el resultado dice si esa regla movió el
     * contador.
     */
    public void registrarProgresoEn(UUID idMision, boolean hizoProgresar) {
        this.idMision = idMision;
        this.hizoProgresarMision = hizoProgresar;
    }

    /**
     * Guarda si esta donación completó la misión, para poder repetir la misma respuesta
     * ante un reintento (punto 14).
     */
    public void registrarSiCompletoMision(boolean completo) {
        this.completMision = completo;
    }
}
