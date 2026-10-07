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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.AbstractAggregateRoot;

/**
 * El donante y su estado de fidelización: categoría actual, misión en curso e insignias.
 *
 * <p>Es el agregado raíz: sin setters, las transiciones pasan por métodos con nombre para
 * no saltarse los eventos de dominio.
 */
@Getter
@Entity
@NoArgsConstructor
public class Perfil extends AbstractAggregateRoot<Perfil> {

    private UUID idUsuario; // id en donaciones

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idPerfil; // id interno

    /** Control de concurrencia optimista: Hibernate lo incrementa en cada {@code UPDATE}. */
    @Version
    private Long version;

    private String nombreUsuario;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoriaActual;

    /**
     * Insignias que ya tiene este donante. Es un {@code Set} para que una misma insignia no
     * se guarde dos veces; depende de que {@link InsigniaObtenida} defina
     * {@code equals}/{@code hashCode}.
     */
    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InsigniaObtenida> insigniasObtenidas;

    /**
     * La misión que el donante está haciendo y cuánto lleva. {@code orphanRemoval} borra el
     * progreso viejo al reemplazarlo en cada cambio de misión o categoría.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ProgresoMision progresoMisionActual;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = null;
        // LinkedHashSet para no perder el orden de obtención.
        this.insigniasObtenidas = new LinkedHashSet<>();
        this.progresoMisionActual = null;
    }

    /**
     * Ubica al donante en una categoría, empezando por su primera misión. Es el alta, así
     * que no dispara eventos.
     *
     * @param categoria la categoría base. Si no tiene misiones, el donante queda sin misión.
     */
    public void iniciarEn(Categoria categoria) {
        this.categoriaActual = categoria;

        Mision primera = categoria == null ? null : categoria.primeraMision();
        this.progresoMisionActual = primera == null ? null : new ProgresoMision(primera);
    }

    /** Deja al donante sin misión cuando terminó la secuencia: no es un error. */
    public void finalizarSecuencia() {
        this.progresoMisionActual = null;
    }

    /** Cambia el nombre del donante. */
    public void cambiarNombre(String nombre) {
        this.nombreUsuario = nombre;
    }

    /** Recalcula la racha de la misión vigente a partir del historial. No hace nada sin misión. */
    public void verificarProgresoMision(List<ImpactoDonacion> donaciones) {
        if (progresoMisionActual != null) {
            progresoMisionActual.evaluarConstancia(donaciones, LocalDateTime.now());
        }
    }

    /**
     * Hace progresar la misión con una donación.
     *
     * @return {@code true} si completó la misión (aunque la insignia ya la tuviera), y hay
     *         que asignarle la siguiente.
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

        // Si ya tenía la insignia, avanza igual pero no la guarda ni notifica de nuevo.
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
     * Asigna una misión nueva y, si corresponde, registra el evento. {@code misionNueva ==
     * null} significa que la categoría se quedó sin misiones, no un error.
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
