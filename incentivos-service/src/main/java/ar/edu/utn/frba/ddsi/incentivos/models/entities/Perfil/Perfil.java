package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import org.springframework.data.domain.AbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Perfil extends AbstractAggregateRoot<Perfil> {

    private UUID idUsuario; // id en donaciones

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idPerfil; // id interno

    private String nombreUsuario;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoriaActual;

    /**
     * Insignias que ya tiene este donante.
     *
     * <p>Es un {@code Set} y no una {@code List} para que la misma insignia no se pueda
     * guardar dos veces (punto 28). El seed pone una misma misión en dos categorías, así
     * que un donante que la complete en una y depois cambie de categoría la vuelve a
     * completar: con una lista guardaba la insignia dos veces, disparaba dos veces el
     * evento de misión completada y el ranking le puntuaba doble.
     *
     * <p>La deduplicación depende de que {@link InsigniaObtenida} tenga
     * {@code equals}/{@code hashCode} por (perfil, insignia); sin eso el {@code Set}
     * compararía por identidad y no reconocería nada.
     */
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InsigniaObtenida> insigniasObtenidas;

    @OneToOne(cascade = CascadeType.ALL)
    private ProgresoMision progresoMisionActual;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = null;
        // LinkedHashSet para no perder el orden de obtención: se expone en el perfil y en
        // el historial, y un HashSet daría un orden arbitrario entre reinicios.
        this.insigniasObtenidas = new LinkedHashSet<>();
        this.progresoMisionActual = null;
    }

    public void verificarProgresoMision(List<ImpactoDonacion> donaciones){
        if (progresoMisionActual != null)
            progresoMisionActual.evaluarConstancia(donaciones, LocalDateTime.now());
    }

    /**
     * Hace progresar la misión con una donación.
     *
     * @return {@code true} si con esto el donante completó la misión, así que hay que
     *         asignarle la siguiente. Ojo: devuelve {@code true} también cuando la
     *         insignia ya la tenía de antes, porque en ese caso igual tiene que avanzar de
     *         misión. Lo que no se repite es el guardado de la insignia ni la notificación.
     */
    public Boolean progresarMision(ImpactoDonacion donacion,
                                   List<ImpactoDonacion> donaciones){
        if (progresoMisionActual == null) return false;

        Mision misionAnterior = progresoMisionActual.getMision();
        Insignia insignia = progresoMisionActual.progresarMision(donacion, donaciones);

        if (insignia == null) {
            return false;
        }

        // El Set decide si la insignia es nueva. Si ya la tenia, el donante completo esta
        // mision en otra categoria: se'avanza igual, pero no hay insignia que guardar ni
        // evento que mandar, porque recibir dos veces la misma notificacion y la misma
        // publicacion seria el bug (punto 28).
        boolean esNueva = insigniasObtenidas.add(new InsigniaObtenida(this, insignia));
        if (!esNueva) {
            return true;
        }

        registerEvent(new MisionCompletada(
                misionAnterior != null ? misionAnterior.getNombreMision() : null,
                insignia.getNombre(),
                this.idUsuario,
                this.nombreUsuario,
                donacion
        ));

        return true;
    }

    /**
 * Asigna una misión nueva y, si corresponde, avisa que cambió.
 *
 * <p>No pide el contacto: el evento lleva el {@code idUsuario} y el listener lo resuelve
     * en {@code AFTER_COMMIT}, fuera de la transacción (punto 12).
 */
public void cambiarMision(Mision misionNueva, Mision misionAnterior) {
        if (misionNueva == null) {
            this.progresoMisionActual = null;
            return;
        }

        this.progresoMisionActual = new ProgresoMision(misionNueva);

        if (misionAnterior != null) {
            registerEvent(new MisionCambiada(
                    misionAnterior.getNombreMision(),
                    misionAnterior.getInsigniaObjetivo().getNombre(),
                    this.nombreUsuario,
                    this.idUsuario,
                    misionNueva.getNombreMision()
            ));
        }
    }

    /** Igual que {@link #cambiarMision}: el contacto lo resuelve el listener. */
    public void cambiarCategoria(Categoria categoriaNueva,
                                 Categoria categoriaAnterior,
                                 Mision misionAnterior) {
        this.categoriaActual = categoriaNueva;

        Mision primeraMision = categoriaNueva != null ? categoriaNueva.primeraMision() : null;
        this.progresoMisionActual = primeraMision != null ? new ProgresoMision(primeraMision) : null;

        if (categoriaAnterior != null && categoriaNueva != null) {
            registerEvent(new CategoriaNuevaPublicar(
                    categoriaAnterior.getNombre(),
                    categoriaNueva.getNombre(),
                    this.nombreUsuario,
                    this.idUsuario
            ));
        }

        if (misionAnterior != null && primeraMision != null) {
            registerEvent(new MisionCambiada(
                    misionAnterior.getNombreMision(),
                    misionAnterior.getInsigniaObjetivo().getNombre(),
                    this.nombreUsuario,
                    this.idUsuario,
                    primeraMision.getNombreMision()
            ));
        }
    }
}
