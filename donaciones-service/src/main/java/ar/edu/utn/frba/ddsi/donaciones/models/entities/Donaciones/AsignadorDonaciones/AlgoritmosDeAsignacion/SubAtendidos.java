package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class SubAtendidos implements AlgoritmoAsignacion {

    @Override
    public List<PropuestaAsignacion> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        String nombreAlgoritmo = this.getClass().getSimpleName();

        /*
         * EXPLICACIÓN DEL MAX-HEAP:
         * Priorizamos a las necesidades con MENOS donaciones en el último trimestre.
         * Usamos .reversed() para convertir el Heap en un Max-Heap.
         * El elemento con el score MÁS ALTO (el que tiene más donaciones, o sea,
         * el "peor" de nuestro Top 10) se queda en la puerta (peek).
         * Si encontramos una propuesta con MENOS donaciones, sacamos al peor y metemos el nuevo.
         */
        PriorityQueue<PropuestaAsignacion> top10 = new PriorityQueue<>(
            Comparator.comparingDouble(PropuestaAsignacion::getScore).reversed()
        );

        for (EntidadBeneficiaria entidad : entidades) {

            // Usamos -1 como flag de evaluación perezosa (lazy evaluation).
            double cantidadDonaciones = -1;

            for (Necesidad necesidad : entidad.getNecesidades()) {

                if (!necesidad.esCompatibleCon(donacion)) {
                    continue;
                }

                // Calculamos el historial de la entidad solo al confirmar compatibilidad
                if (cantidadDonaciones == -1) {
                    cantidadDonaciones = (double) cantidadDonacionesUltimoTrimestre(entidad);
                }

                if (top10.size() < 10) {
                    agregarPropuesta(top10, entidad, necesidad, cantidadDonaciones);
                } else if (cantidadDonaciones < top10.peek().getScore()) {
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