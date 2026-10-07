package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class SubAtendidos implements AlgoritmoAsignacion {

    /** Prioriza a las entidades con menos donaciones en el último trimestre, con un
     *  Max-Heap de 10 (el score más alto queda en el peek y sale primero). */
    @Override
    public List<PropuestaAsignacion> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        String nombreAlgoritmo = this.getClass().getSimpleName();

        PriorityQueue<PropuestaAsignacion> top10 = new PriorityQueue<>(
            Comparator.comparingDouble(PropuestaAsignacion::getScore).reversed()
        );

        for (EntidadBeneficiaria entidad : entidades) {
            // El historial se calcula una sola vez por entidad, con inicialización perezosa.
            double cantidadDonaciones = -1;

            for (Necesidad necesidad : entidad.getNecesidades()) {
                if (!necesidad.esCompatibleCon(donacion)) {
                    continue;
                }

                if (cantidadDonaciones == -1) {
                    cantidadDonaciones = (double) cantidadDonacionesUltimoTrimestre(entidad);
                }

                if (top10.size() < 10) {
                    agregarPropuesta(top10, entidad, necesidad, cantidadDonaciones);
                } else if (cantidadDonaciones < top10.peek().getScore()) {
                    // Una entidad más sub-atendida que la peor del top-10 la reemplaza.
                    reemplazarPeorPropuesta(top10, entidad, necesidad, cantidadDonaciones);
                }
            }
        }
        return extraerRanking(top10, nombreAlgoritmo);
    }

    private int cantidadDonacionesUltimoTrimestre(EntidadBeneficiaria entidad) {
        LocalDate haceUnTrimestre = LocalDate.now().minusMonths(3);
        return (int) entidad.verDonaciones().stream()
                            .filter(d -> d.getFechaEntrega() != null)
                            .filter(d -> d.getFechaEntrega().isAfter(haceUnTrimestre))
                            .count();
    }
}