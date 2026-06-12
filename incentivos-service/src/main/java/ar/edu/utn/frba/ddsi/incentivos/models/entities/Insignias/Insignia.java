package ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class Insignia {
    private String nombre;
    private String descripcion;
    private String urlImagen;
    private LocalDate fechaObtencion;

    public Insignia(String nombreInsignia, String NombreMisionCompletada){
        this.nombre = nombreInsignia;
        this.fechaObtencion = null;
        this.urlImagen = null;
        this.descripcion = NombreMisionCompletada;
    }

    public InsigniaDTO toDTO() {
        InsigniaDTO dto = new InsigniaDTO();
        dto.setNombre(this.nombre);
        dto.setFechaObtencion(this.fechaObtencion);
        dto.setDescripcion(this.descripcion);
        dto.setUrlImagen(this.urlImagen);
        return dto;
    }
}
