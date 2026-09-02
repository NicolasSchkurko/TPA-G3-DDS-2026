package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Mision {
    private UUID idMision; // id interno
    private UUID idAdmin;
    private String nombreMision;
    private String descripcion;
    private Insignia insigniaObjetivo;
    private Regla reglaDeProgreso;

    public Mision(String nombre,
                  UUID idAdmin,
                  String descripcion,
                  String nombreInsignia,
                  Regla regla) {
        this.idMision = UUID.randomUUID();
        this.idAdmin = idAdmin;
        this.nombreMision = nombre;
        this.descripcion = descripcion;
        this.insigniaObjetivo = new Insignia(nombreInsignia, nombre);
        this.reglaDeProgreso = regla;
    }

}
