package ar.edu.utn.frba.ddsi.logisticas.dto.chofer;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChoferDTO {
    private UUID idChofer;

    @NotBlank(message = "es obligatorio")
    private String nombre;

    private String email;
    private Boolean disponible;
    private Boolean activo;

    /** Arma el chofer a persistir con un id nuevo. Si no vino {@code disponible}, queda disponible. */
    public Chofer toDomain() {
        boolean esDisponible = this.disponible == null || this.disponible;
        Chofer chofer = new Chofer(java.util.UUID.randomUUID(), this.nombre, esDisponible);
        chofer.setEmail(this.email);
        return chofer;
    }

    /** Copia los datos del chofer sobre este DTO. El chofer no puede ser {@code null}. */
    public ChoferDTO aplicar(Chofer chofer) {
        this.idChofer = chofer.getIdChofer();
        this.nombre = chofer.getNombre();
        this.email = chofer.getEmail();
        this.disponible = chofer.isDisponible();
        this.activo = chofer.isActivo();
        return this;
    }
}
