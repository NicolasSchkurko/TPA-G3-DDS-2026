package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.dto.MisionDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
public abstract class Mision {
    private List<ImpactoDonacion> donaciones;
    private String nombreMision;
    private Insignia insigniaObjetivo;
    private Integer progresoActual;
    private Integer progresoObjetivo;

    public Mision(Insignia insignia, String descripcion){
        this.nombreMision = descripcion;
        this.insigniaObjetivo = insignia;
        this.donaciones = new ArrayList<>();
        this.progresoActual = 0;
        this.progresoObjetivo = null;
    }

    public MisionDTO toDTO() {
        MisionDTO dto = new MisionDTO();
        dto.setNombreMision(this.nombreMision);
        if (this.insigniaObjetivo != null) {
            dto.setInsigniaObjetivo(this.insigniaObjetivo.toDTO());
        }
        dto.setProgresoActual(this.progresoActual);
        dto.setProgresoObjetivo(this.progresoObjetivo);
        return dto;
    }

    //override de este metodo en cada mision
    public void registrarProgreso(ImpactoDonacion donacion) {
        donaciones.add(donacion);
        progresoActual++;
    }

    public Boolean estaCompleta() {
        return Objects.equals(progresoActual, progresoObjetivo);
    }
}
