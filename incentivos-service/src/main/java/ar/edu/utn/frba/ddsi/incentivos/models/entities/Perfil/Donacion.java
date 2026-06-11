package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donacion {
    private UUID idDonacion;
    private LocalDate fechaEntrega;
    private Integer cantidadBienes;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;

    public Donacion(String entidadBeneficiaria,
                    Integer cantidadBienes,
                    LocalDate fechaEntrega,
                    String categoria,
                    String estado,
                    UUID idDonacion){
        this.idDonacion = idDonacion;
        this.estado = estado;
        this.entidadBeneficiaria = entidadBeneficiaria;
        this.cantidadBienes = cantidadBienes;
        this.fechaEntrega = fechaEntrega;
        this.categoria = categoria;
    }
}
