package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaNuevaPublicar;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

/**
 * El donante y todo su estado de fidelización: en qué categoría está, qué misión está
 * haciendo y qué insignias ya tiene.
 *
 * <p>Es el agregado raíz, así que <b>no tiene setters</b>. Todas las transiciones pasan por
 * métodos con nombre: {@link #iniciarEn} alCSFaltar, {@link #cambiarMision} y
 * {@link #cambiarCategoria} al avanzar, {@link #progresarMision} al donar y
 * {@link #finalizarSecuencia} cuando se agotó el programa.
 *
 * <p>La razón es concreta: con setters abiertos, {@code PerfilService} podía llamar
 * {@code setProgresoMisionActual(...)} y saltarse los eventos de dominio. El donante
 * avanzaba de misión sin que nadie publicara {@code MisionCambiada}, así que no le
 * llegaban ni la notificación ni la publicación.
 */
@Getter
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

    /**
     * Ubica al donante en una categoría, empezando por su primera misión.
     *
     * <p>Es el alta del perfil, así que no dispara ningún evento: no hay una misión
     * anterior de la que "cambiar". Lo que sí hace es dejar al donante con las reglas de la
     * categoría ya aplicadas, que antes las tenía que armar el service a mano.
     *
     * @param categoria la categoría base. Si no tiene misiones, el donante queda sin
     *                  misión en vez de con una en null.
     */
    public void iniciarEn(Categoria categoria) {
        this.categoriaActual = categoria;

        Mision primera = categoria == null ? null : categoria.primeraMision();
        this.progresoMisionActual = primera == null ? null : new ProgresoMision(primera);
    }

    /**
     * Deja al donante sin misión.
     *
     * <p>Se usa cuando terminó la secuencia: no hay más categorías ni más misiones que
     * ofrecerle. No es un error, es un donante que ya hizo todo lo que el programa de
     * fidelización tenía para darle.
     */
    public void finalizarSecuencia() {
        this.progresoMisionActual = null;
    }

    /**
     * Cambia el nombre del donante.
     *
     * <p>Con setter público esto se podía llamar desde cualquier lado, y el nombre es lo
     * que aparece en el perfil, en el ranking y en las publicaciones: dejarlo abierto
     * hacía muy fácil que quedara inconsistente con el de {@code donaciones-service}.
     */
    public void cambiarNombre(String nombre) {
        this.nombreUsuario = nombre;
    }

    /**
     * Recalcula la racha de la misión vigente a partir de todo su historial.
     *
     * <p>No hace nada si el donante no tiene misión: un donante sin misión no puede tener
     * racha que recalcular.
     */
    public void verificarProgresoMision(List<ImpactoDonacion> donaciones) {
        if (progresoMisionActual != null) {
            progresoMisionActual.evaluarConstancia(donaciones, LocalDateTime.now());
        }
    }

    /**
     * Hace progresar la misión con una donación.
     *
     * @return {@code true} si con esto el donante completó la misión, así que hay que
     *         asignarle la siguiente. Ojo: devuelve {@code true} también cuando la
     *         insignia ya la tenía de antes, porque en ese caso igual tiene que avanzar de
     *         misión. Lo que no se repite es el guardado de la insignia ni la notificación.
     */
    public boolean progresarMision(ImpactoDonacion donacion,
                                   List<ImpactoDonacion> donaciones) {
        if (progresoMisionActual == null) {
            return false;
        }

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
