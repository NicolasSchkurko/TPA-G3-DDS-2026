package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.AlgoritmoAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Asignador de donaciones.
 *
 * Procesa donaciones mediante múltiples algoritmos, calcula intersecciones de propuestas
 * y asigna scores basándose en la suma de posiciones.
 */

public class AsignadorDonaciones {

    private final List<AlgoritmoAsignacion> algoritmos;
    private final List<ResultadoMatchmaking> donacionesPendientesDeAprobacion;
    private GestorMatchmaking gestorMatchmaking;
    private GestorDonaciones gestorDonaciones;
    private GestorNecesidades gestorNecesidades;

    public AsignadorDonaciones(RepositorioDeResultadosMatchmaking resultadosMatchmakings, GestorDonaciones gestorDonaciones, GestorNecesidades gestorNecesidades) {
        this.algoritmos = new ArrayList<>();
        this.donacionesPendientesDeAprobacion = new ArrayList<>();
        this.gestorMatchmaking = gestorMatchmaking;
        this.gestorDonaciones = gestorDonaciones;
        this.gestorNecesidades = gestorNecesidades;
    }

    public List<AlgoritmoAsignacion> getAlgoritmos() {
        return Collections.unmodifiableList(algoritmos);
    }

    public void ejecutarMatchmakingBatch(List<Donacion> todasLasDonaciones, List<EntidadBeneficiaria> todasLasEntidades) {
        if (todasLasDonaciones == null || todasLasEntidades == null) {
            return;
        }
        todasLasDonaciones.forEach(donacion -> procesarMatchmaking(donacion, todasLasEntidades));
    }

    /**
     * Procesa una donación:
     * 1. Ejecuta algoritmos y obtiene una lista plana de propuestas.
     * 2. Calcula la intersección y la devuelve ya consolidada y enriquecida.
     * 3. Si NO hay intersección, devuelve las propuestas originales intactas.
     */
    public void procesarMatchmaking(Donacion donacion, List<EntidadBeneficiaria> todasLasEntidades) {
        validarAlgoritmosConfigurados();

        // 1. Ejecutar todos los algoritmos devolviendo una única lista plana
        List<PropuestaAsignacion> todasLasPropuestas = ejecutarTodosLosAlgoritmos(donacion, todasLasEntidades);

        // 2. Calcular la intersección consolidada
        List<PropuestaAsignacion> interseccion = obtenerInterseccion(todasLasPropuestas);
        boolean huboCoincidenciaTotal = !interseccion.isEmpty();

        List<PropuestaAsignacion> resultadoFinal;

        // 3. Procesar según la existencia de intersección
        if (huboCoincidenciaTotal) {
            resultadoFinal = interseccion;
        } else {
            // Si no hay intersección, ordenar por posición original y devolverlas nativas
            todasLasPropuestas.sort(Comparator.comparingInt(PropuestaAsignacion::getPosicion));
            resultadoFinal = todasLasPropuestas;
        }

        // 4. Procesar resultado final
        procesarResultadoFinal(donacion, resultadoFinal, huboCoincidenciaTotal);
    }

    public List<ResultadoMatchmaking> getDonacionesPendientesDeAprobacion() {
        return this.donacionesPendientesDeAprobacion;
    }

public void confirmarAsignacion(ResultadoMatchmaking resultado, PropuestaAsignacion propuestaElegida) {
    if (resultado == null || propuestaElegida == null) {
        throw new IllegalArgumentException("Resultado y propuesta no pueden ser nulos.");
    }

    Donacion donacion = resultado.getDonacion();

    if (donacion.getEstado() != Estado.PENDIENTE_ASIGNACION) {
        throw new IllegalStateException("La donación no está en estado pendiente.");
    }

    // Validar que la propuesta pertenece a este resultado
    if (!resultado.getPropuestasOrdenadas().contains(propuestaElegida)) {
        throw new IllegalArgumentException("La propuesta elegida no pertenece a este matching.");
    }

    asignarDonacionAPropuesta(donacion, propuestaElegida);
    donacionesPendientesDeAprobacion.remove(resultado);
}
    // --------------------------------------------------
    // Métodos privados
    // --------------------------------------------------

    private void validarAlgoritmosConfigurados() {
        if (algoritmos.isEmpty()) {
            throw new IllegalStateException("No hay algoritmos configurados.");
        }
    }

    /**
     * Ejecuta todos los algoritmos y recolecta todo en una sola lista.
     */
    private List<PropuestaAsignacion> ejecutarTodosLosAlgoritmos(
        Donacion donacion,
        List<EntidadBeneficiaria> todasLasEntidades) {

        List<PropuestaAsignacion> listaPlana = new ArrayList<>();
        for (AlgoritmoAsignacion algoritmo : this.algoritmos) {
            listaPlana.addAll(algoritmo.rankear(donacion, todasLasEntidades));
        }
        return listaPlana;
    }

    /**
     * Agrupa, filtra la intersección y consolida los datos en una sola lista final.
     * Todo el uso de Maps queda contenido y aislado aquí dentro.
     */
    private List<PropuestaAsignacion> obtenerInterseccion(List<PropuestaAsignacion> todasLasPropuestas) {
        int totalAlgoritmos = this.algoritmos.size();

        List<PropuestaAsignacion> interseccionEnriquecida = todasLasPropuestas.stream()
                                                                              .collect(Collectors.groupingBy(p -> p.getNecesidad().getId())) // Agrupa por UUID
                                                                              .values().stream()
                                                                              .filter(apariciones -> apariciones.size() == totalAlgoritmos) // Filtra las que están en todos
                                                                              .map(this::consolidarApariciones) // Convierte cada grupo en 1 propuesta consolidada
                                                                              .sorted(Comparator.comparingDouble(PropuestaAsignacion::getScore)) // Ordena por score
                                                                              .collect(Collectors.toList());

        // Asignamos el índice final
        for (int i = 0; i < interseccionEnriquecida.size(); i++) {
            interseccionEnriquecida.get(i).setPosicion(i + 1);
        }

        return interseccionEnriquecida;
    }

    /**
     * Toma las apariciones de una misma necesidad en distintos algoritmos y las fusiona.
     */
    private PropuestaAsignacion consolidarApariciones(List<PropuestaAsignacion> apariciones) {
        PropuestaAsignacion representativa = apariciones.get(0);

        // Determinar la mejor aparición individual de esta propuesta
        PropuestaAsignacion mejorAparicion = apariciones.stream()
                                                        .min(Comparator.comparingInt(PropuestaAsignacion::getPosicion))
                                                        .orElse(representativa);

        // En la intersección, el score es simplemente la suma de posiciones
        double scoreNumerico = apariciones.stream().mapToInt(PropuestaAsignacion::getPosicion).sum();

        return new PropuestaAsignacion(
            representativa.getEntidad(),
            representativa.getNecesidad(),
            "Interseccion",
            0, // Posición final a determinar después de ordenar
            scoreNumerico
        );
    }

    private void procesarResultadoFinal(
        Donacion donacion,
        List<PropuestaAsignacion> resultadoFinal,
        boolean huboCoincidenciaTotal) {

        if (huboCoincidenciaTotal && resultadoFinal.size() == 1) {
            PropuestaAsignacion propuestaUnica = resultadoFinal.get(0);
            asignarDonacionAPropuesta(donacion, propuestaUnica);
        } else {
            registrarDonacionPendienteDeAprobacion(donacion, resultadoFinal, huboCoincidenciaTotal);
        }
    }

    //Tambien delegue las resposabilidades a los gestores y repos
    public void asignarDonacionAPropuesta(Donacion donacion, PropuestaAsignacion propuesta) {
        //donacion.setEntidad(propuesta.getEntidad());
        gestorDonaciones.asignarEntidad(donacion.getId(), propuesta.getEntidad());
        //donacion.setEstado(Estado.ASIGNADO);
        gestorDonaciones.cambiarEstado(donacion.getId(), "ASIGNADO", "Donacion Asignada");

        registrarDonacionEnNecesidad(donacion, propuesta.getNecesidad());
    }

    //Aca cambie que el gestor de necesidades se encargue de cargarle la donacion
    private void registrarDonacionEnNecesidad(Donacion donacion, Necesidad necesidad) {
        gestorNecesidades.agregarDonacionANecesidad(necesidad.getId(), donacion);
    }

    //cambie que el gestor se encargue de cambiar el estado y que el repo guarde los resultados
    private void registrarDonacionPendienteDeAprobacion(
        Donacion donacion,
        List<PropuestaAsignacion> resultadoFinal,
        boolean huboCoincidenciaTotal) {

        //donacion.setEstado(Estado.PENDIENTE_ASIGNACION);
        gestorDonaciones.cambiarEstado(donacion.getId(), "PENDIENTE_ASIGNACION", "Añadida a un resultadoMatchmaking");

        ResultadoMatchmaking resultado = new ResultadoMatchmaking(
            donacion,
            resultadoFinal,
            huboCoincidenciaTotal
        );

        gestorMatchmaking.guardarResultado(resultado);
    }

    public void limpiarDonacionesPendientesDeAprobacion(){
        donacionesPendientesDeAprobacion.clear();
    }
}