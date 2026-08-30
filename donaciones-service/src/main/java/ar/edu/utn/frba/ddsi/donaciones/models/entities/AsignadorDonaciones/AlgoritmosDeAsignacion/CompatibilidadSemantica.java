package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion;


import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class CompatibilidadSemantica implements AlgoritmoAsignacion {

    @Override
    public List<PropuestaAsignacion> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        String nombreAlgoritmo = this.getClass().getSimpleName();

        /*
         * EXPLICACIÓN DEL MIN-HEAP (PriorityQueue):
         * * Un Heap es una estructura de datos que se auto-ordena parcialmente.
         * En este caso usamos un Min-Heap: se configura para que la propuesta con el
         * MENOR score siempre se quede en la "cima" o "puerta" (accesible vía peek()).
         * * ¿Por qué usar esto y no instanciar todo y hacer un Collections.sort()?
         * Rendimiento. Si hay 100.000 necesidades compatibles, hacer un .sort()
         * obligaría a instanciar 100.000 objetos en memoria y ejecutar un algoritmo
         * de ordenamiento pesado (O(N log N)), para luego tirar 99.990 a la basura.
         * * Con el Min-Heap limitado a 10, recorremos las 100.000 necesidades
         * ejecutando solo matemática (costo casi nulo). Si encontramos un score
         * que es MEJOR que el PEOR de nuestro top 10 (el que está en el peek()),
         * lo instanciamos, echamos al peor, y metemos el nuevo.
         * Resultado: Memoria casi intacta (máximo 10 objetos) y velocidad O(N).
         */
        PriorityQueue<PropuestaAsignacion> top10 = new PriorityQueue<>(
            Comparator.comparingDouble(PropuestaAsignacion::getScore)
        );

        // 1. Iteración clásica de alto rendimiento sin instanciar objetos basura
        for (EntidadBeneficiaria entidad : entidades) {
            for (Necesidad necesidad : entidad.getNecesidades()) {

                // Filtro 1: Descartamos inmediatamente si no es compatible
                if (!necesidad.esCompatibleCon(donacion)) {
                    System.out.println("donacion no compatible con necesidad");
                    continue;
                }

                double score = calcularScore(necesidad, donacion);

                // Filtro 2: Descartamos si el score no es útil
                if (score <= 0) {
                    continue;
                }

                // A partir de este punto, iteramos solo sobre "los útiles"
                if (top10.size() < 10) {
                    agregarPropuesta(top10, entidad, necesidad, score);
                } else if (score > top10.peek().getScore()) {
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