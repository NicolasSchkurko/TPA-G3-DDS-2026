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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistorialActividad {
    private UUID idActividad;
    private UUID idPerfil;
    private List<ActividadMensual> actividadPorMes;

    public HistorialActividad(UUID idPerfil, List<ActividadMensual> actividadPorMes) {
        this.idActividad = UUID.randomUUID();
        this.idPerfil = idPerfil;
        setActividadPorMes(actividadPorMes);
    }

    /**
     * Agrega una donacion al mes correspondiente a su fecha de entrega.
     */
    public void agregarDonacion(ImpactoDonacion donacion) {
        if (donacion == null || donacion.getFechaEntrega() == null) {
            throw new IllegalArgumentException("La donacion debe tener fecha de entrega");
        }

        YearMonth mes = YearMonth.from(donacion.getFechaEntrega());
        ActividadMensual actividad = actividadPorMes.stream()
                .filter(a -> a.getPeriodo().equals(mes))
                .findFirst()
                .orElseGet(() -> {
                    ActividadMensual nueva = new ActividadMensual(mes, new ArrayList<>());
                    actividadPorMes.add(nueva);
                    actividadPorMes.sort(Comparator.comparing(ActividadMensual::getPeriodo));
                    return nueva;
                });
        actividad.agregarDonacion(donacion);
    }

    /** Compara el total del atributo entre dos meses. */
    public Metricas calcularMetricasDeterminada(
            Function<ImpactoDonacion, Integer> keyExtractor,
            YearMonth mes1, YearMonth mes2){
        Objects.requireNonNull(keyExtractor, "El atributo numerico es obligatorio");
        Objects.requireNonNull(mes1, "El primer mes es obligatorio");
        Objects.requireNonNull(mes2, "El segundo mes es obligatorio");
        return new Metricas(mes1, mes2, Metricas.calcularVariacion(totalDelMes(mes1, keyExtractor), totalDelMes(mes2, keyExtractor)));
    }

    /** Devuelve la variacion de cada mes respecto del mes anterior registrado. */
    public List<Metricas> calcularMetricasMensuales(
            Function<ImpactoDonacion, Integer> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "El atributo numerico es obligatorio");
        List<Metricas> metricas = new ArrayList<>();
        for (int i = 1; i < actividadPorMes.size(); i++) {
            YearMonth actual = actividadPorMes.get(i).getPeriodo();
            YearMonth anterior = actividadPorMes.get(i - 1).getPeriodo();
            metricas.add(calcularMetricasDeterminada(keyExtractor, actual, anterior));
        }
        return metricas;
    }

    /** Evolucion porcentual entre el inicio y el fin del periodo indicado. */
    public Double calcularEvolucionPorPeriodo(
            Function<ImpactoDonacion, Integer> keyExtractor, YearMonth inicio, YearMonth fin) {
        Objects.requireNonNull(keyExtractor, "El atributo numerico es obligatorio");
        Objects.requireNonNull(inicio, "El primer mes es obligatorio");
        Objects.requireNonNull(fin, "El segundo mes es obligatorio");
        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException("El inicio no puede ser posterior al fin");
        }
        return Metricas.calcularVariacion(totalDelMes(fin, keyExtractor), totalDelMes(inicio, keyExtractor));
    }

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
        Map<YearMonth, List<ImpactoDonacion>> donacionesPorMes = (actividadPorMes == null ? List.<ActividadMensual>of() : actividadPorMes)
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
