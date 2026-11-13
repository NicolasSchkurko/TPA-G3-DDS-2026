package ar.edu.utn.frba.ddsi.logisticas.dto.camion;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CamionDTO {
    private UUID idChofer;

    @NotBlank(message = "es obligatorio")
    private String patente;

    @NotNull(message = "es obligatorio")
    private Double capacidadVolumen; //m2

    @NotNull(message = "es obligatorio")
    private Double altura; //m

    @NotNull(message = "es obligatorio")
    private Double capacidadCarga;

    private Boolean disponible;

    /** Arma el camión a persistir. Si no vino {@code disponible}, queda disponible. */
    public Camion toDomain() {
        Boolean disponible = this.disponible != null ? this.disponible : Boolean.TRUE;
        return new Camion(this.patente, this.capacidadVolumen, this.altura,
                this.capacidadCarga, disponible);
    }

    /** Copia los datos del camión sobre este DTO. El camión no puede ser {@code null}. */
    public CamionDTO aplicar(Camion camion) {
        if (camion.getChofer() != null) {
            this.idChofer = camion.getChofer().getIdChofer();
        }
        this.patente = camion.getPatente();
        this.capacidadVolumen = camion.getCapacidadVolumen();
        this.altura = camion.getAltura();
        this.capacidadCarga = camion.getCapacidadCarga();
        this.disponible = camion.getDisponible();
        return this;
    }
}