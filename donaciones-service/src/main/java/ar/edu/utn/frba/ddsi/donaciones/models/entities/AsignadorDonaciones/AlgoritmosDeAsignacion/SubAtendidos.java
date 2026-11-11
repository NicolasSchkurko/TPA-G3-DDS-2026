package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

public class SubAtendidos implements AlgoritmoAsignacion {

    private final Map<UUID, Integer> conteosPorEntidad;

    public SubAtendidos() {
        this(null);
    }

    public SubAtendidos(Map<UUID, Integer> conteosPorEntidad) {
        this.conteosPorEntidad = conteosPorEntidad;
    }

    /** Prioriza a las entidades con menos donaciones en el último trimestre, con un
     *  Max-Heap de 10 (el score más alto queda en el peek y sale primero). */
    @Override
    public List<PropuestaAsignacion> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades,
                                            Map<UUID, Integer> cantidadesVigentesPorNecesidad) {
        String nombreAlgoritmo = this.getClass().getSimpleName();

        /*
         * EXPLICACIÓN DEL MAX-HEAP:
         * Priorizamos a las necesidades con MENOS donaciones en el último trimestre.
         * Usamos .reversed() para convertir el Heap en un Max-Heap.
         * El elemento con el score MÁS ALTO (el que tiene más donaciones, o sea,
         * el "peor" de nuestro Top 10) se queda en la puerta (peek).
         * Si encontramos una propuesta con MENOS donaciones, sacamos al peor y metemos el nuevo.
         */
        // thenComparing por ID de necesidad: determinista y no depende del orden en que
        // vienen las entidades (antes, dos elementos con el mismo score quedaban desempatados
        // por el orden arbitrario del heap).
        PriorityQueue<PropuestaAsignacion> top10 = new PriorityQueue<>(
            Comparator.comparingDouble(PropuestaAsignacion::getScore).reversed()
                      .thenComparing(p -> p.getNecesidad().getId())
        );

        for (EntidadBeneficiaria entidad : entidades) {
            // El historial se calcula una sola vez por entidad, con inicialización perezosa.
            double cantidadDonaciones = -1;

            for (Necesidad necesidad : entidad.getNecesidades()) {
                boolean compatible = cantidadesVigentesPorNecesidad == null
                    ? necesidad.esCompatibleCon(donacion)
                    : necesidad.esCompatibleCon(donacion,
                        cantidadesVigentesPorNecesidad.getOrDefault(necesidad.getId(), 0));
                if (!compatible) {
                    continue;
                }

                if (cantidadDonaciones == -1) {
                    cantidadDonaciones = (double) cantidadDonacionesUltimoTrimestre(entidad);
                }

                if (top10.size() < 10) {
                    agregarPropuesta(top10, entidad, necesidad, cantidadDonaciones);
                } else if (cantidadDonaciones <= top10.peek().getScore()) {
                    // <= (no <): con una comparación estricta, una entidad con 0 donaciones
                    // llenaba el top10 entero y ninguna otra entidad con el mismo score (0)
                    // podía desplazarla nunca. Con <=, el desempate lo decide el thenComparing
                    // del heap en vez del orden de llegada de las entidades.
                    reemplazarPeorPropuesta(top10, entidad, necesidad, cantidadDonaciones);
                }
            }
        }
        return extraerRanking(top10, nombreAlgoritmo);
    }

    private int cantidadDonacionesUltimoTrimestre(EntidadBeneficiaria entidad) {
        if (conteosPorEntidad != null) {
            return conteosPorEntidad.getOrDefault(entidad.getId(), 0);
        }
        LocalDate haceUnTrimestre = LocalDate.now().minusMonths(3);
        return (int) entidad.verDonaciones().stream()
                            .filter(d -> d.getEstado() == Estado.ENTREGADO)
                            .filter(d -> d.getFechaEntrega() != null)
                            .filter(d -> d.getFechaEntrega().isAfter(haceUnTrimestre))
                            .count();
    }
}