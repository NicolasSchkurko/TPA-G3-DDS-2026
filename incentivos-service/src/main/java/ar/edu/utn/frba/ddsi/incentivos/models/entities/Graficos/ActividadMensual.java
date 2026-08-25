package ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ActividadMensual {
    private YearMonth periodo;
    private List<ImpactoDonacion> donacionesEnMes;

    public ActividadMensual(YearMonth periodo, List<ImpactoDonacion> donacionesEnMes) {
        if (periodo == null) {
            throw new IllegalArgumentException("El periodo de actividad es obligatorio");
        }
        this.periodo = periodo;
        this.donacionesEnMes = donacionesEnMes == null ? new ArrayList<>() : donacionesEnMes;
    }

    public void agregarDonacion(ImpactoDonacion donacion) {
        if (donacion == null || donacion.getFechaEntrega() == null) {
            throw new IllegalArgumentException("La donacion debe tener fecha de entrega");
        }
        if (!periodo.equals(YearMonth.from(donacion.getFechaEntrega()))) {
            throw new IllegalArgumentException("La fecha de entrega no corresponde al periodo de actividad");
        }
        donacionesEnMes.add(donacion);
    }

    public int totalizar(
            Function<ImpactoDonacion, Integer> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "El atributo numerico es obligatorio");
        return donacionesEnMes.stream()
                .map(keyExtractor)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    public List<String> entidadesBeneficiadas() {
        List<String> entidades = new ArrayList<>();
        donacionesEnMes.stream()
                .map(ImpactoDonacion::getEntidadBeneficiaria)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(entidad -> !entidad.isEmpty())
                .forEach(entidades::add);
        return entidades;
    }
}
