package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.UnidadTiempo;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * El avance de un donante en una misión: todo lo que la regla necesita recordar del donante
 * vive acá y no en la misión compartida.
 */
@Entity
@Getter
@NoArgsConstructor
public class ProgresoMision implements ProgresoDelDonante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Mision mision;

    private Integer progreso;

    /**
     * Valores del atributo que este donante ya vio, para las reglas del tipo "N valores
     * distintos". Es un {@code Set} para que el mismo valor no cuente dos veces.
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
     * Guarda el valor si es nuevo y todavía no se alcanzó la cantidad que pide la regla.
     * Acotarlo evita que el set crezca indefinidamente en misiones de larga duración.
     */
    @Override
    public boolean registrarValorObservado(String valor) {
        int maximo = maxValoresObservados();
        if (valoresObservados.size() >= maximo && !valoresObservados.contains(valor)) {
            return false;
        }
        return valoresObservados.add(valor);
    }

    /** Cuántos valores distintos necesita la operación; para otras reglas no hay tope. */
    private int maxValoresObservados() {
        if (mision.getReglaDeProgreso().getOperacion() instanceof ValoresDistintos valores) {
            return valores.getCantValoresDistintos() == null
                    ? 0
                    : valores.getCantValoresDistintos();
        }
        return Integer.MAX_VALUE;
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
     * Calcula la racha de una misión con constancia contando meses calendario consecutivos
     * hacia atrás desde la última donación. Ordena y filtra el historial completo.
     */
    public void evaluarConstancia(List<ImpactoDonacion> donaciones,
                                   LocalDateTime fechaEvaluacion) {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null) {
            return;
        }
        if (donaciones.isEmpty()) {
            reiniciarProgreso();
            return;
        }

        List<ImpactoDonacion> donacionesQueProgresaron = donaciones.stream()
                .filter(d -> Boolean.TRUE.equals(d.getHizoProgresarMision()))
                .sorted(Comparator.comparing(ImpactoDonacion::getFechaEntrega))
                .toList();

        evaluarConstanciaOrdenada(donacionesQueProgresaron, fechaEvaluacion);
    }

    /**
     * Igual que {@link #evaluarConstancia}, pero recibe solo las donaciones que progresaron,
     * ordenadas por fecha ascendente. Es el camino del scheduler, que ya las trae así de la
     * consulta y evita filtrar y ordenar 500 historiales por pasada.
     */
    public void evaluarConstanciaOrdenada(
            List<ImpactoDonacion> donacionesQueProgresaron,
            LocalDateTime fechaEvaluacion) {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null) {
            return;
        }
        if (donacionesQueProgresaron.isEmpty()) {
            reiniciarProgreso();
            return;
        }

        // Defensa por si el llamador no las trae ordenadas: el camino normal ya lo hace.
        List<ImpactoDonacion> ordenadas = donacionesQueProgresaron;
        if (!estaOrdenadaPorFecha(ordenadas)) {
            ordenadas = ordenadas.stream()
                    .sorted(Comparator.comparing(ImpactoDonacion::getFechaEntrega))
                    .toList();
        }

        ImpactoDonacion ultima = ordenadas.get(ordenadas.size() - 1);

        // En meses se compara por mes calendario, no por instante: si donó el 1 de octubre
        // con cadencia mensual, todavía tiene todo noviembre para mantener la racha.
        boolean caduco = constancia.getUnidadTiempo() == UnidadTiempo.MONTHS
                ? YearMonth.from(fechaEvaluacion).isAfter(
                        YearMonth.from(ultima.getFechaEntrega())
                                .plusMonths(constancia.getCantidad()))
                : fechaEvaluacion.isAfter(ultima.getFechaEntrega()
                        .plus(constancia.getCantidad(),
                                constancia.getUnidadTiempo().comoChronoUnit()));

        if (caduco) {
            // La racha caduco: el donante arranca de cero.
            reiniciarProgreso();
            return;
        }

        // Recorrido del mes más nuevo al más viejo: mesPrevio es siempre posterior.
        YearMonth mesPrevio = null;
        int mesesConsecutivos = 0;

        for (int i = ordenadas.size() - 1; i >= 0; i--) {
            YearMonth mes = YearMonth.from(ordenadas.get(i).getFechaEntrega());

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

    private static boolean estaOrdenadaPorFecha(List<ImpactoDonacion> donaciones) {
        for (int i = 1; i < donaciones.size(); i++) {
            if (donaciones.get(i - 1).getFechaEntrega()
                    .isAfter(donaciones.get(i).getFechaEntrega())) {
                return false;
            }
        }
        return true;
    }

    /** Si el avance acumulado cumple el objetivo de la misión. */
    public boolean estaCompleta() {
        return mision.getReglaDeProgreso().estaCompleta(progreso, this);
    }

    /**
     * Aplica la regla a una donación y registra el resultado en la propia donación, que la
     * constancia necesita después para reconstruir la racha.
     *
     * @return si esta donación hizo progresar la misión.
     */
    public boolean evaluarProgreso(ImpactoDonacion donacion) {
        Object valorAtributo = mision.getReglaDeProgreso().aplicar(donacion);
        boolean hizoProgresar = mision.getReglaDeProgreso().operar(valorAtributo, this);

        donacion.registrarProgresoEn(mision.getIdMision(), hizoProgresar);

        return hizoProgresar;
    }

    /**
     * Aplica una donación al avance y devuelve la insignia si con esto se completó la
     * misión. Con constancia el avance lo decide {@link #evaluarConstancia}; sin ella, uno
     * por donación.
     *
     * @return la insignia a otorgar, o {@code null} si la misión sigue sin completarse.
     */
    public Insignia progresarMision(ImpactoDonacion donacion,
                                    List<ImpactoDonacion> donaciones) {
        boolean hizoProgresar = evaluarProgreso(donacion);

        if (mision.getReglaDeProgreso().getConstancia() != null) {
            List<ImpactoDonacion> donacionesEvaluar = new ArrayList<>(donaciones);
            donacionesEvaluar.add(donacion);
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
     * Deja el avance en cero: contador y valores observados, juntos para no dejar el avance
     * a medias.
     */
    public void reiniciarProgreso() {
        progreso = 0;
        limpiarValoresObservados();
    }
}
