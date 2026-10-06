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
 * Una misión del programa: qué hay que hacer, con qué regla se cuenta y qué insignia
 * otorga al completarla.
 *
 * <p>Es dueño de su insignia objetivo (cascade) pero solo referencia su regla, que viene
 * armada desde la factory. Cada misión crea su propia fila de insignia, así que el mismo
 * nombre puede repetirse entre misiones sin chocar.
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
     * Qué tiene que cumplir el donante para completar esta misión.
     *
     * <p>El {@code orphanRemoval} cubre las tres filas que se acumulaban al editar una
     * misión (punto 17): la {@code Regla} anterior, y con ella su {@code ReglaConstancia} y
     * su {@code Operacion}, porque el {@code cascade = ALL} de la regla incluye {@code REMOVE}.
     * Antes, cada vez que el admin cambiaba el criterio de completado quedaban las tres
     * filas viejas en la base sin que nadie las referenciara.
     *
     * <p>Es seguro porque una {@code Regla} y su {@code Operacion} no se comparten: cada
     * misión construye las suyas en {@code MisionFactory}.
     *
     * <p><b>Acá NO va {@code orphanRemoval} en cambio, a diferencia de
     * {@code ProgresoMision}:</b> la {@code Insignia} es referenciada por el
     * {@code InsigniaObtenida} de todos los donantes que ya la obtuvieron, así que borrarla
     * por ser huérfana rompería la FK de esas filas o, peor, se llevaría por delante las
     * insignias ya otorgadas. Hoy el código nunca reemplaza la referencia —
     * {@code Mision.actualizar} la modifica en el lugar con
     * {@code Insignia.actualizar} — pero si alguna vez lo hiciera, el borrado tiene que ser
     * explícito y verificado, no un efecto colateral de un cambio de anotación.
     */
    @OneToOne(cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "regla_id")
    private Regla reglaDeProgreso;

    public Mision(String nombre,
                  UUID idAdmin,
                  String descripcion,
                  String nombreInsignia,
                  Regla regla) {
        this.idAdmin = idAdmin;
        this.nombreMision = nombre;
        this.descripcion = descripcion;
        this.insigniaObjetivo = new Insignia(nombreInsignia, nombre);
        this.reglaDeProgreso = regla;
    }

    /**
     * Aplica los cambios de una misión.
     *
     * <p>La insignia se actualiza sobre la insignia que ya existe, y no se reemplaza por
     * la que trae el DTO, para no dejar filas huérfanas ni romper las insignias que los
     * perfiles ya obtuvieron.
     *
     * @return {@code true} si cambió lo que el donante tiene que cumplir. Es lo que
     *         permite no borrarles el avance a todos los que estaban en la misión solo
     *         porque se retocó el texto (punto 15).
     */
    public boolean actualizar(Mision misionModificada) {
        if (misionModificada.getNombreMision() != null) {
            this.nombreMision = misionModificada.getNombreMision();
        }

        if (misionModificada.getDescripcion() != null) {
            this.descripcion = misionModificada.getDescripcion();
        }

        if (misionModificada.getInsigniaObjetivo() != null
                && misionModificada.getInsigniaObjetivo().getNombre() != null) {

            Insignia insigniaNueva = misionModificada.getInsigniaObjetivo();
            String nombreNuevo = insigniaNueva.getNombre();
            // La descripción es la de la INSIGNIA. Antes se pasaba this.descripcion, que
            // es la de la misión, y por eso cada edición dejaba la insignia objetivo con
            // el texto de la misión.
            String descripcionNueva = insigniaNueva.getDescripcion();

            this.insigniaObjetivo.actualizar(nombreNuevo, descripcionNueva);
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
