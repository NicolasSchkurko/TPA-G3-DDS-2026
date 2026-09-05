package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Mision {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idMision; // id interno

    private UUID idAdmin;
    private String nombreMision;
    private String descripcion;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "insignia_objetivo_id", nullable = false)
    private Insignia insigniaObjetivo;

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "regla_id")
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
