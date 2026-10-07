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

    /** {@code Set.add} devuelve {@code true} solo si el valor era nuevo. */
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
     * Calcula la racha de una misión con constancia contando meses calendario consecutivos
     * hacia atrás desde la última donación. {@code cantidad} y {@code unidadTiempo} definen
     * cuánto puede pasar antes de que la racha caduque.
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

        // Solo cuentan las donaciones que hicieron progresar la misión, en orden de fecha.
        List<ImpactoDonacion> donacionesQueProgresaron = donaciones.stream()
                .filter(d -> Boolean.TRUE.equals(d.getHizoProgresarMision()))
                .sorted(Comparator.comparing(ImpactoDonacion::getFechaEntrega))
                .toList();

        if (donacionesQueProgresaron.isEmpty()) {
            reiniciarProgreso();
            return;
        }

        ImpactoDonacion ultima = donacionesQueProgresaron.get(donacionesQueProgresaron.size() - 1);
        LocalDateTime limite = ultima.getFechaEntrega()
                .plus(constancia.getCantidad(), constancia.getUnidadTiempo());

        if (fechaEvaluacion.isAfter(limite)) {
            // La racha caduco: el donante arranca de cero.
            reiniciarProgreso();
            return;
        }

        // Recorrido del mes más nuevo al más viejo: mesPrevio es siempre posterior.
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
     * Deja el avance en cero: contador y valores observados, juntos para no dejar el avance
     * a medias.
     */
    public void reiniciarProgreso() {
        progreso = 0;
        limpiarValoresObservados();
    }
}
