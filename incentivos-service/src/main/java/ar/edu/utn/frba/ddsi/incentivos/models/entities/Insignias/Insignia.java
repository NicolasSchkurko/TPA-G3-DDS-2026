package ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.dto.InsigniaDTO;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class Insignia {
    String nombreInsignia;
    LocalDate fecha;
    Mision misionCompletada;

    public Insignia(String nombreInsignia, Mision misionCompletada){
        this.nombreInsignia = nombreInsignia;
        this.fecha = null;
        this.misionCompletada = misionCompletada;
    }

    public InsigniaDTO toDTO() {
        InsigniaDTO dto = new InsigniaDTO();
        dto.setNombre(this.nombreInsignia);
        dto.setFechaObtencion(this.fecha);
        dto.setDescripcion(null);
        dto.setUrlImagen(null);
        return dto;
    }
}
