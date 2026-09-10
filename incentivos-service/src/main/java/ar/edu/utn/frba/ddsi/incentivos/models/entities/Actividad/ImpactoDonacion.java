package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idDonacion; // id interno
    private UUID idUsuario; // id de donaciones
    private LocalDateTime fechaEntrega;
    private Integer cantidadBienes;
    private String subCategoria;
    private String categoria;
    private String entidadBeneficiaria;
    private String estado;
    private Boolean hizoProgresarMision = false; //indica si hizo progresar la mision actual
    private UUID idMision; //en el momento en que ingreso esta donacion, el perfil tenia una mision asignada, asi q le asigno el id de esa mision

    public ImpactoDonacion(String entidadBeneficiaria,
                           Integer cantidadBienes,
                           LocalDateTime fechaEntrega,
                           String subCategoria,
                           String categoria,
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
