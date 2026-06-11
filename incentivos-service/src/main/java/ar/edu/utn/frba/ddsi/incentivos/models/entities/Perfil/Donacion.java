package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donacion {
    private String entidadBeneficiaria;
    private Integer cantidadBienes;
    private LocalDate fechaEntrega;
    private String categoria;

    public Donacion(String entidadBeneficiaria, Integer cantidadBienes, LocalDate fechaEntrega, String categoria){
        this.entidadBeneficiaria = entidadBeneficiaria;
        this.cantidadBienes = cantidadBienes;
        this.fechaEntrega = fechaEntrega;
        this.categoria = categoria;
    }
}
