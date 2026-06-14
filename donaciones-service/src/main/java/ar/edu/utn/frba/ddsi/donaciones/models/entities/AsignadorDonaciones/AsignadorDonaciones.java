package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estados;
import java.util.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AlgoritmosDeAsignacion.AlgoritmoAsignacion;

public class AsignadorDonaciones {
    private static AsignadorDonaciones instanciaUnica;

    private List<AlgoritmoAsignacion> algoritmos;
    private Map<Donacion, ResultadoMatchmaking> donacionesPendientesDeAprobacion;

    private AsignadorDonaciones() {
        this.donacionesPendientesDeAprobacion = new HashMap<>();
        this.algoritmos = new ArrayList<>();
    }

    public static AsignadorDonaciones getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new AsignadorDonaciones();
        }
        return instanciaUnica;
    }

    // -----------------------------------------------------------------------------------------
    // Fase 1: Ejecución por Lotes con Filtrado en Memoria (Streams)
    // -----------------------------------------------------------------------------------------

    public void ejecutarMatchmakingBatch(List<Donacion> todasLasDonaciones, List<EntidadBeneficiaria> todasLasEntidades) {
        todasLasDonaciones.forEach(donacion -> procesarMatchmaking(donacion, todasLasEntidades));
    }

    // -----------------------------------------------------------------------------------------
    // Fase 2: Procesamiento Individual y Cambio de Estado Intermedio
    // -----------------------------------------------------------------------------------------

    public void procesarMatchmaking(Donacion donacion, List<EntidadBeneficiaria> todasLasEntidades) {
        if (algoritmos.isEmpty()) {
            throw new IllegalStateException("No hay algoritmos configurados.");
        }

        Map<String, List<EntidadBeneficiaria>> resultadosPorAlgoritmo = new HashMap<>();

        for (AlgoritmoAsignacion algoritmo : this.algoritmos) {
            String nombreAlgoritmo = algoritmo.getClass().getSimpleName();
            List<EntidadBeneficiaria> ranking = algoritmo.rankear(donacion, todasLasEntidades);
            resultadosPorAlgoritmo.put(nombreAlgoritmo, ranking);
        }

        List<EntidadBeneficiaria> interseccion = calcularInterseccionMultiple(resultadosPorAlgoritmo.values());

        // Caso 1: Auto-asignación (Pasa a estado final directamente)
        if (interseccion.size() == 1) {
            asignarDonacion(donacion, interseccion.get(0));
            return;
        }

        // Caso 2: Intervención requerida (Pasa a estado intermedio)
        donacion.setEstado(Estados.PENDIENTE_ASIGNACION);

        ResultadoMatchmaking resultado;
        if (interseccion.size() > 1) {
            resultado = new ResultadoMatchmaking(interseccion, true);
        } else {
            resultado = new ResultadoMatchmaking(resultadosPorAlgoritmo, false);
        }

        donacionesPendientesDeAprobacion.put(donacion, resultado);
    }

    private List<EntidadBeneficiaria> calcularInterseccionMultiple(Collection<List<EntidadBeneficiaria>> coleccionDeListas) {
        if (coleccionDeListas == null || coleccionDeListas.isEmpty()) {
            return new ArrayList<>();
        }

        Iterator<List<EntidadBeneficiaria>> iterador = coleccionDeListas.iterator();
        Set<EntidadBeneficiaria> interseccion = new HashSet<>(iterador.next());

        while (iterador.hasNext()) {
            interseccion.retainAll(new HashSet<>(iterador.next()));
        }

        return new ArrayList<>(interseccion);
    }

    // -----------------------------------------------------------------------------------------
    // Fase 3: Aprobación Manual
    // -----------------------------------------------------------------------------------------

    public Map<Donacion, ResultadoMatchmaking> getDonacionesPendientesDeAprobacion() {
        return this.donacionesPendientesDeAprobacion;
    }

    public void confirmarAsignacion(Donacion donacion, EntidadBeneficiaria entidadElegida) {
        if (donacionesPendientesDeAprobacion.containsKey(donacion) && donacion.getEstado() == Estados.PENDIENTE_ASIGNACION) {
            asignarDonacion(donacion, entidadElegida);
            donacionesPendientesDeAprobacion.remove(donacion);
        } else {
            throw new IllegalStateException("La donación no está pendiente de asignación.");
        }
    }

    private void asignarDonacion(Donacion donacion, EntidadBeneficiaria entidadElegida){
        donacion.setEntidad(entidadElegida);
        donacion.setEstado(Estados.ASIGNADO);
        //falta lo de asignarlo a la necesidad
    }
}