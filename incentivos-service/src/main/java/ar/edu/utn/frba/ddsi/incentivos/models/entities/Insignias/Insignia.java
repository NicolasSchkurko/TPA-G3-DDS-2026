package ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias;

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

    public Insignia(String nombreInsignia, String descripcion){
        this.nombre = nombreInsignia;
        this.fechaObtencion = null;
        this.urlImagen = "https://donatrack.org/images/insignias/" + nombreInsignia + ".png"; // Una URL base por nombre de insignia
        this.descripcion = descripcion;
    }
}