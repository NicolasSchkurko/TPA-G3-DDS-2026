package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * El avance de <b>un</b> donante en <b>una</b> misión.
 *
 * <p>Es el agregado del avance: todo lo que la regla necesita recordar del donante vive
 * acá y en ningún otro lado. Antes, las operaciones que tenían que recordar qué valores
 * había visto el donante (como {@code ValoresDistintos}) guardaban esa lista en la
 * entidad de la misión, que es compartida por todos los que la hacen, y por eso la
 * misión se completaba antes de tiempo para todos.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProgresoMision implements ProgresoDelDonante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Mision mision;

    private Integer progreso;

    /**
     * Valores del atributo de la regla que este donante ya vio. Los necesitan las reglas
     * del tipo "N valores distintos".
     *
     * <p>Es un {@code Set} a propósito: que el mismo valor no cuente dos veces lo
     * garantiza el tipo. La restricción única sobre {@code (progreso_mision_id, valor)}
     * también lo garantiza a nivel base de datos.
     *
     * <p>Es {@code LAZY} por defecto. Todos sus usos corren dentro de transacciones
     * ({@code calcularProgreso}, {@code estaCompleta} y {@code evaluarConstancia}), así
     * que no hay riesgo de LazyInitializationException.
     */
    @ElementCollection
    @CollectionTable(
            name = "valor_observado",
            joinColumns = @JoinColumn(name = "progreso_mision_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_valor_observado_progreso_valor",
                    columnNames = {"progreso_mision_id", "valor"}))
    @Column(name = "valor", nullable = false, length = 512)
    private Set<String> valoresObservados = new LinkedHashSet<>();

    public ProgresoMision(Mision mision) {
        this.mision = mision;
        this.progreso = 0;
    }

    /**
     * {@code Set.add} devuelve {@code true} solo si el valor era nuevo, que es justo
     * lo que promete este método.
     */
    @Override
    public boolean registrarValorObservado(String valor) {
        return valoresObservados.add(valor);
    }

    @Override
    public int cantidadValoresObservados() {
        return valoresObservados.size();
    }

    /** Descarta los valores ya vistos, porque el donante arranca de cero con la racha. */
    public void limpiarValoresObservados() {
        valoresObservados.clear();
    }

    public void evaluarConstancia(List<ImpactoDonacion> donaciones,
                                   LocalDateTime fechaEvaluacion) {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null) return;
        if (donaciones.isEmpty()) {
            reiniciar();
            return;
        }

        int progresoActual = 0;
        LocalDateTime anterior = null;

//las donaciones a evaluar constancia deben ser las que hayan hecho progresar la mision actual
        List<ImpactoDonacion> donacionesEvaluar = donaciones.stream()
                .filter(donacion -> Boolean.TRUE.equals(
                        donacion.getHizoProgresarMision())
                )
                .toList();

        for (ImpactoDonacion donacion : donacionesEvaluar) {
            LocalDateTime limite = anterior == null
                    ? null
                    : anterior.plus(constancia.getCantidad(), constancia.getUnidadTiempo());

            if (limite != null && donacion.getFechaEntrega().isAfter(limite)) {
                progresoActual = 0;
            }

            progresoActual++;
            anterior = donacion.getFechaEntrega();
        }

        LocalDateTime limite = anterior == null
                ? null
                : anterior.plus(constancia.getCantidad(), constancia.getUnidadTiempo());
        progreso = limite != null && fechaEvaluacion.isAfter(limite)
                ? 0
                : progresoActual;

        // Los valores observados son de la racha vigente, no de la historia completa. Si
        // la racha se rompio, el donante va de cero y tampoco le cuentan los valores que
        // vio antes del corte; si no los borramos, "3 categorias distintas" se completaba
        // con categorias de una racha que ya no existe.
        if (progreso == 0) {
            limpiarValoresObservados();
        }
    }

    public boolean estaCompleta() {
        return mision.getReglaDeProgreso().estaCompleta(progreso, this);
    }

    public boolean evaluarProgreso(ImpactoDonacion donacion) {
        donacion.setIdMision(mision.getIdMision());
        Object valorAtributo = mision.getReglaDeProgreso().aplicar(donacion);
        boolean hizoProgresar = mision.getReglaDeProgreso().operar(valorAtributo, this);
        donacion.setHizoProgresarMision(hizoProgresar);
        return hizoProgresar;
    }

    // Se ha removido PosicionRanking de los parámetros.
    // Esa actualización debe manejarse mediante un EventListener que escuche MisionCompletada.
    public Insignia progresarMision(ImpactoDonacion donacion,
                                    List<ImpactoDonacion> donaciones) {
        boolean hizoProgresar = evaluarProgreso(donacion); //la donacion se

        if (mision.getReglaDeProgreso().getConstancia() != null) {
            List<ImpactoDonacion> donacionesEvaluar = new ArrayList<>(donaciones);
            donacionesEvaluar.add(donacion);
            donacionesEvaluar.sort(Comparator.comparing(ImpactoDonacion::getFechaEntrega));
            evaluarConstancia(donacionesEvaluar, donacion.getFechaEntrega());
        } else if (hizoProgresar) {
            progreso++;
        }

        if (this.estaCompleta()) {
            return this.getMision().getInsigniaObjetivo();
        }
        return null;
    }

    /**
     * Deja el avance en cero: contador y valores observados. Se usa cuando la racha se
     * rompe y también cuando el admin cambia el criterio de la misión (punto 15), así que
     * las dos cosas tienen que caer juntas o el avance queda a medias.
     */
public void reiniciarProgreso() {
        reiniciar();
    }

    private void reiniciar() {
        progreso = 0;
        limpiarValoresObservados();
    }
}
