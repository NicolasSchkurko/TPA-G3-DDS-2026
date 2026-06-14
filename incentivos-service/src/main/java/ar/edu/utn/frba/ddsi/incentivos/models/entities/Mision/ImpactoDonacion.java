package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import java.time.LocalDate;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImpactoDonacion {
    private UUID idDonacion; // id interno
    private UUID idUsuario; // id de donaciones
    private LocalDate fechaEntrega;
    private Integer cantidadBienes;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;

    public ImpactoDonacion(String entidadBeneficiaria,
                           Integer cantidadBienes,
                           LocalDate fechaEntrega,
                           String categoria,
                           String estado,
                           UUID idUsuario){
        this.idDonacion = UUID.randomUUID();
        this.idUsuario = idUsuario;
        this.estado = estado;
        this.entidadBeneficiaria = entidadBeneficiaria;
        this.cantidadBienes = cantidadBienes;
        this.fechaEntrega = fechaEntrega;
        this.categoria = categoria;
    }
}
