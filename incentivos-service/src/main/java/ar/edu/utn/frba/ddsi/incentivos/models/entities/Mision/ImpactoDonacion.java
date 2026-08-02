package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImpactoDonacion {
    private UUID idDonacion; // id interno
    private UUID idUsuario; // id de donaciones
    private LocalDateTime fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;

    public ImpactoDonacion(String entidadBeneficiaria,
                           Integer cantidadBienes,
                           LocalDateTime fechaEntrega,
                           String subCategoria,
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
        this.subCategoria = subCategoria;
    }
}
