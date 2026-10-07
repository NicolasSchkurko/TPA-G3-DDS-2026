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
        int cantidadFaltante = necesidad.getCantidadObjetivo() - necesidad.cantidadRecibida();
        int cantidadDonada = donacion.sumaCantidadBienes();
        return cantidadDonada <= cantidadFaltante
               ? (double) cantidadDonada / cantidadFaltante
               : (double) cantidadFaltante / cantidadDonada;
    }
}