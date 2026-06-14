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
        this.urlImagen = "https://donatrack.org/images/insignias/" + NombreMisionCompletada + ".png"; // Una URL base por nombre de mision
        this.descripcion = NombreMisionCompletada;
    }


}
