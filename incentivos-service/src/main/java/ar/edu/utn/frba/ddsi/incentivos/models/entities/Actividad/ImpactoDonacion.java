package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
    private UUID idDonacion;

    private UUID idUsuario; // id de donaciones
    private LocalDateTime fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;
    private Boolean hizoProgresarMision = false; //indica si hizo progresar la mision actual
    private UUID idMision; //en el momento en que ingreso esta donacion, el perfil tenia una mision asignada, asi q le asigno el id de esa mision

    /**
     * Si esta donacion completo la mision del donante. Se guarda para poder repetir la
     * misma respuesta cuando llega un reintento: un endpoint idempotente no puede devolver
     * un resultado distinto la segunda vez, porque el cliente ya recibio una respuesta y
     * si cambiara lo tomaria por un fallo.
     */
    private Boolean completMision = false;

    public ImpactoDonacion(String entidadBeneficiaria,
                           Integer cantidadBienes,
                           LocalDateTime fechaEntrega,
                           String categoria,
                           String subCategoria,
                           String estado,
                           UUID idUsuario){
        this.idUsuario = idUsuario;
        this.estado = estado;
        this.entidadBeneficiaria = entidadBeneficiaria;
        this.cantidadBienes = cantidadBienes;
        this.fechaEntrega = fechaEntrega;
        this.categoria = categoria;
        this.subCategoria = subCategoria;
    }
}
