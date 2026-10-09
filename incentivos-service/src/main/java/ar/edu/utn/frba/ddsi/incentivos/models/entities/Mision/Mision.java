package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una misión del programa: qué hay que hacer, con qué regla se cuenta y qué insignia otorga.
 * Es dueño de su insignia objetivo (cascade); cada misión crea su propia fila de insignia.
 */
@Getter
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

    /**
     * Qué tiene que cumplir el donante para completar esta misión. El {@code orphanRemoval}
     * borra la regla vieja (con su constancia y operación) al reemplazarla. La insignia, en
     * cambio, se modifica en el lugar: ya está referenciada por insignias obtenidas.
     */
    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "regla_id")
    private Regla reglaDeProgreso;

    /** Atajo para cuando solo se tiene el nombre de la insignia. */
    public Mision(String nombre,
                  UUID idAdmin,
                  String descripcion,
                  String nombreInsignia,
                  Regla regla) {
        this(nombre, idAdmin, descripcion, nombreInsignia, null, null, regla);
    }

    /**
     * Crea la misión con su insignia objetivo.
     *
     * @param descripcionInsignia el texto propio de la insignia, separado de la descripción
     *                           de la misión.
     * @param urlImagenInsignia   la imagen de la insignia. Acepta null.
     */
    public Mision(String nombre,
                  UUID idAdmin,
                  String descripcion,
                  String nombreInsignia,
                  String descripcionInsignia,
                  String urlImagenInsignia,
                  Regla regla) {
        this.idAdmin = idAdmin;
        this.nombreMision = nombre;
        this.descripcion = descripcion;
        this.insigniaObjetivo =
                new Insignia(nombreInsignia, descripcionInsignia, urlImagenInsignia);
        this.reglaDeProgreso = regla;
    }

    /**
     * Aplica los cambios de una misión. La insignia se actualiza en el lugar para no romper
     * las insignias ya obtenidas.
     *
     * @return {@code true} si cambió lo que el donante tiene que cumplir, así se evita
     *         borrar el avance solo por un retoque de texto.
     */
    public boolean actualizar(Mision misionModificada) {
        if (misionModificada.getNombreMision() != null) {
            this.nombreMision = misionModificada.getNombreMision();
        }

        if (misionModificada.getDescripcion() != null) {
            this.descripcion = misionModificada.getDescripcion();
        }

        if (misionModificada.getInsigniaObjetivo() != null) {

            Insignia insigniaNueva = misionModificada.getInsigniaObjetivo();
            // Los tres datos son los de la insignia, no los de la misión.
            this.insigniaObjetivo.actualizar(
                    insigniaNueva.getNombre(),
                    insigniaNueva.getDescripcion(),
                    insigniaNueva.getUrlImagen()
            );
        }

        Regla reglaModificada = misionModificada.getReglaDeProgreso();
        if (reglaModificada == null) {
            return false;
        }

        if (this.reglaDeProgreso != null
                && this.reglaDeProgreso.esEquivalenteA(reglaModificada)) {
            // La regla pide lo mismo: se conserva la existente en vez de guardar una
            // copia nueva, que dejaría la regla y su operación anteriores huérfanas.
            return false;
        }

        this.reglaDeProgreso = reglaModificada;
        return true;
    }
}
