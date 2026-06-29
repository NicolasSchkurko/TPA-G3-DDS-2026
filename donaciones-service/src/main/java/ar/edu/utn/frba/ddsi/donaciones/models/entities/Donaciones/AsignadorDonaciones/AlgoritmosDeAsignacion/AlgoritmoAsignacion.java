package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public interface AlgoritmoAsignacion {
    List<PropuestaAsignacion> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades);
    // Métod0 para agregar directamente cuando hay espacio
    default void agregarPropuesta(
        PriorityQueue<PropuestaAsignacion> top10,
        EntidadBeneficiaria entidad,
        Necesidad necesidad,
        double score) {
        PropuestaAsignacion nueva = new PropuestaAsignacion(entidad, necesidad);
        nueva.setScore(score);
        top10.offer(nueva);
    }

    // Métod0 para reemplazar cuando el Top 10 está lleno
    default void reemplazarPeorPropuesta(PriorityQueue<PropuestaAsignacion> top10,
                                         EntidadBeneficiaria entidad,
                                         Necesidad necesidad,
                                         double score) {
        top10.poll();
        agregarPropuesta(top10, entidad, necesidad, score); // Reutilizamos el métod0 anterior
    }

    // Métod0 default para abstraer la conversión del Heap a la lista ordenada final
    // Como el Heap saca primero el PEOR del top 10, al insertarlo siempre
    // en la posición 0 vamos empujándolo hacia atrás. Al terminar,
    // el último en salir (el MEJOR) quedará en la posición 0.
    // Al mismo tiempo, aprovechamos para setear los metadatos.
    default List<PropuestaAsignacion> extraerRanking(PriorityQueue<PropuestaAsignacion> top10, String nombreAlgoritmo) {
        List<PropuestaAsignacion> ranking = new ArrayList<>();

        while (!top10.isEmpty()) {
            PropuestaAsignacion propuesta = top10.poll();
            propuesta.setAlgoritmo(nombreAlgoritmo);
            propuesta.setPosicion(top10.size() + 1);
            ranking.add(0, propuesta);
        }

        return ranking;
    }
}