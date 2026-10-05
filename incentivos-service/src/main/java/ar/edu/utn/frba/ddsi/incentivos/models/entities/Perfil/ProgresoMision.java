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
import java.time.YearMonth;
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

    /**
     * Calcula la racha de una misión con constancia.
     *
     * <p><b>La racha se cuenta en meses calendario, no en donaciones</b> (punto 26). Antes
     * la única condición era "esta donación no tiene más de {@code cantidad} unidades de
     * antigüedad que la anterior", o sea que {@code cantidad} se usaba como margen en
     * días. Con la misión "Realiza 1 donación durante 3 meses consecutivos"
     * ({@code constancia = (1, MONTHS)}), tres donaciones en tres días consecutivos
     * completaban la misión de tres meses.
     *
     * <p>Ahora se cuentan los meses calendario consecutivos hacia atrás desde el mes de la
     * última donación: dos donaciones en el mismo mes cuentan una sola vez, y un mes sin
     * donate cierra la racha ahí.
     *
     * <p>La {@code cantidad} y la {@code unidadTiempo} siguen teniendo un papel: definen
     * cuánto puede pasar desde la última donación antes de que la racha caduque. Con
     * {@code (1, MONTHS)} el donante tiene que donar al menos una vez por mes, que es
     * justamente lo que dice el enunciado de la misión.
     */
    public void evaluarConstancia(List<ImpactoDonacion> donaciones,
                                   LocalDateTime fechaEvaluacion) {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null) return;
        if (donaciones.isEmpty()) {
            reiniciar();
            return;
        }

        // Para la racha solo cuentan las donaciones que hicieron progresar esta mision, y en
        // orden de fecha porque la cuenta de meses va hacia atras.
        List<ImpactoDonacion> donacionesQueProgresaron = donaciones.stream()
                .filter(d -> Boolean.TRUE.equals(d.getHizoProgresarMision()))
                .sorted(Comparator.comparing(ImpactoDonacion::getFechaEntrega))
                .toList();

        if (donacionesQueProgresaron.isEmpty()) {
            reiniciar();
            return;
        }

        ImpactoDonacion ultima = donacionesQueProgresaron.get(donacionesQueProgresaron.size() - 1);
        LocalDateTime limite = ultima.getFechaEntrega()
                .plus(constancia.getCantidad(), constancia.getUnidadTiempo());

        if (fechaEvaluacion.isAfter(limite)) {
            // La racha caduco: el donante arranca de cero.
            reiniciar();
            return;
        }

        // Cuenta meses calendario consecutivos hacia atras desde el mes de la ultima
        // donacion. El recorrido es del mes mas nuevo al mas viejo, asi que mesPrevio
        // es SIEMPRE posterior a mes.
        YearMonth mesPrevio = null;
        int mesesConsecutivos = 0;

        for (int i = donacionesQueProgresaron.size() - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(donacionesQueProgresaron.get(i).getFechaEntrega());

            if (mesPrevio != null) {
                if (mes.equals(mesPrevio)) {
                    // Varias donaciones en el mismo mes: ese mes ya esta contado.
                    continue;
                }
                if (!mes.plusMonths(1).equals(mesPrevio)) {
                    // Hay un mes sin donacion entre medio: la racha se corta aca.
                    break;
                }
            }

            mesesConsecutivos++;
            mesPrevio = mes;
        }

        progreso = mesesConsecutivos;
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
