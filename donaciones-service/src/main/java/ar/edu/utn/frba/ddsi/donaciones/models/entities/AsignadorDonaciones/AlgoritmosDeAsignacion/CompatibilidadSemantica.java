package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion;


import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class CompatibilidadSemantica implements AlgoritmoAsignacion {

    /** Top-10 con un Min-Heap limitado en vez de ordenar todo: se recorre una vez (O(N))
     *  y en memoria quedan como máximo los 10 mejores. */
    @Override
    public List<PropuestaAsignacion> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        String nombreAlgoritmo = this.getClass().getSimpleName();

        PriorityQueue<PropuestaAsignacion> top10 = new PriorityQueue<>(
            Comparator.comparingDouble(PropuestaAsignacion::getScore)
        );

        for (EntidadBeneficiaria entidad : entidades) {
            for (Necesidad necesidad : entidad.getNecesidades()) {
                if (!necesidad.esCompatibleCon(donacion)) {
                    continue;
                }

                double score = calcularScore(necesidad, donacion);
                if (score <= 0) {
                    continue;
                }

                if (top10.size() < 10) {
                    agregarPropuesta(top10, entidad, necesidad, score);
                } else if (score > top10.peek().getScore()) {
                    // Reemplaza al peor del top-10: el peek del Min-Heap es siempre el menor.
                    reemplazarPeorPropuesta(top10, entidad, necesidad, score);
                }
            }
        }

        return extraerRanking(top10, nombreAlgoritmo);
    }

    private double calcularScore(Necesidad necesidad, Donacion donacion) {
        // cantidadFaltante() usa la misma ventana que esCompatibleCon()/estaSatisfecha(): para
        // una NecesidadRecurrente eso es cantidadRecibidaEnPeriodo(), no el histórico completo.
        // Antes medían contra columnas distintas: una recurrente ya llenada en el pasado quedaba
        // "compatible" (período en 0) pero con score <= 0 (histórico ya cubierto), así que el
        // filtro de la línea de arriba la descartaba para siempre.
        int cantidadFaltante = necesidad.cantidadFaltante();
        int cantidadDonada = donacion.sumaCantidadBienes();

        // Blindaje de división por cero: una donación con suma de bienes 0 (o una necesidad sin
        // nada pendiente, score <= 0 ya la filtra el caller) no debe producir NaN, que al no
        // cumplir "score <= 0" se cuela en el PriorityQueue y rompe el orden del heap.
        if (cantidadFaltante <= 0 || cantidadDonada <= 0) {
            return 0;
        }

        return cantidadDonada <= cantidadFaltante
               ? (double) cantidadDonada / cantidadFaltante
               : (double) cantidadFaltante / cantidadDonada;
    }
}