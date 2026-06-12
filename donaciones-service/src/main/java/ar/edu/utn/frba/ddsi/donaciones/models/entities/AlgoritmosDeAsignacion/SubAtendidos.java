package ar.edu.utn.frba.ddsi.donaciones.models.entities.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class SubAtendidos implements AlgoritmoAsignacion {

    @Override
    public List<EntidadBeneficiaria> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        return entidades.stream()
                .filter(entidad -> tieneNecesidadCompatible(entidad, donacion))
                .sorted((e1, e2) -> Integer.compare(
                        cantidadDonacionesUltimoTrimestre(e1),
                        cantidadDonacionesUltimoTrimestre(e2)))
                .limit(10)
                .collect(Collectors.toList());
    }

    private boolean tieneNecesidadCompatible(EntidadBeneficiaria entidad, Donacion donacion) {
        return entidad.getNecesidades().stream()
                .filter(n -> !n.estaSatisfecha())
                .anyMatch(n -> n.getSubcategoria().equals(donacion.getSubcategoria()));
    }

    private int cantidadDonacionesUltimoTrimestre(EntidadBeneficiaria entidad) {
        LocalDate haceUnTrimestre = LocalDate.now().minusMonths(3);
        return (int) entidad.verDonaciones().stream()
                .filter(d -> d.getFechaEntrega() != null)
                .filter(d -> d.getFechaEntrega().isAfter(haceUnTrimestre))
                .count();
    }
}