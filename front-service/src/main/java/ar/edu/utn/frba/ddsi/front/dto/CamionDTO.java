package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CamionDTO {
    private UUID idChofer;

    @NotBlank(message = "Escriba la patente del camion")
    private String patente;

    @NotNull(message = "Escriba la capacidad por volumen del camion")
    private Double capacidadVolumen; //m2

    @NotNull(message = "Escriba la altura del camion")
    private Double altura; //m

    @NotNull(message = "Escriba la capacidad de carga del camion")
    private Double capacidadCarga;

    @NotNull(message = "Escriba si el camion comienza disponible")
    private Boolean disponible;

    public CamionDTO(
            String patente,
            Double capacidadVolumen,
            Double altura,
            Double capacidadCarga,
            Boolean disponible) {

        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.disponible = disponible;
    }
}