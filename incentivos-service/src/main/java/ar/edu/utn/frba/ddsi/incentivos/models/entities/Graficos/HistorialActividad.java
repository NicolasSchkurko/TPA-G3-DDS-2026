package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "historial_actividad")
public class HistorialActividad {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idActividad;
    private UUID idPerfil;

    @OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @JoinColumn(name = "historial_id")
    private List<ImpactoDonacion> historialDonaciones= new ArrayList<>();

    public HistorialActividad(UUID idPerfil, List<ImpactoDonacion> historialDonaciones) {
        this.idActividad = UUID.randomUUID();
        this.idPerfil = idPerfil;
        setHistorialDonaciones(historialDonaciones);
    }

    /**
     * Agrega una donacion al mes correspondiente a su fecha de entrega.
     */
    public void agregarDonacion(ImpactoDonacion donacion) {
        historialDonaciones.add(donacion);
        historialDonaciones.sort(Comparator.comparing(ImpactoDonacion::getFechaEntrega));
    }

    /** Devuelve la variacion de cada mes respecto del mes anterior registrado. */
    public List<Metricas> calcularMetricasMensuales(
            Function<ImpactoDonacion, Integer> keyExtractor) {
        List<Metricas> metricas = new ArrayList<>();
        /* Compara el total del atributo entre dos meses. */
        for (int i = 1; i < actividadPorMes.size(); i++) {
            YearMonth actual = actividadPorMes.get(i).getPeriodo();
            YearMonth anterior = actividadPorMes.get(i - 1).getPeriodo();
            Metricas m = new Metricas(
                    actual, anterior,
                    Metricas.calcularVariacion(totalDelMes(actual, keyExtractor), totalDelMes(anterior, keyExtractor))
            );
            metricas.add(m);
        }
        return metricas;
    }


/*
    Evolucion porcentual entre el inicio y el fin del periodo indicado.
    public Double calcularEvolucionPorPeriodo(
            Function<ImpactoDonacion, Integer> keyExtractor, YearMonth inicio, YearMonth fin) {
        if (inicio.isAfter(fin)) {
            return null;
        }
        return Metricas.calcularVariacion(totalDelMes(fin, keyExtractor), totalDelMes(inicio, keyExtractor));
    }
*/

    public int cantidadDonacionesTotales() {
        return actividadPorMes.stream()
                .mapToInt(a -> a.getDonacionesEnMes().size())
                .sum();
    }

    public int cantidadEntidadesBeneficiadas() {
        List<String> entidades = actividadPorMes.stream()
                .map(ActividadMensual::entidadesBeneficiadas)
                .flatMap(List::stream)
                .toList();
        return entidades.size();
    }

    //el setter para este atributo no es suficiente, asi q usar este
    public void setActividadPorMes(List<ActividadMensual> actividadPorMes) {
        Map<YearMonth, List<ImpactoDonacion>> donacionesPorMes =
                (actividadPorMes == null ? List.<ActividadMensual>of() : actividadPorMes)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        ActividadMensual::getPeriodo,
                        Collectors.flatMapping(a -> a.getDonacionesEnMes().stream(), Collectors.toList())));

        this.actividadPorMes = donacionesPorMes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ActividadMensual(entry.getKey(), entry.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private int totalDelMes(YearMonth mes,
                            Function<ImpactoDonacion, Integer> keyExtractor) {
        return actividadPorMes.stream()
                .filter(actividad -> actividad.getPeriodo().equals(mes))
                .mapToInt(actividad -> actividad.totalizar(keyExtractor))
                .sum();
    }
}
