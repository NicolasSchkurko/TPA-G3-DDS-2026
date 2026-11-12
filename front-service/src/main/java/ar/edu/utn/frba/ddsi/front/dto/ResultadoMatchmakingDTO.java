package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado del asignador para una donación: la donación en cuestión y el ranking de entidades
 * candidatas. Espejo del ResultadoMatchmakingDTO de donaciones-service.
 *
 * <p>GET /api/donaciones/pendientes devuelve exactamente estos resultados, y existe uno por cada
 * donación que quedó en PENDIENTE_ASIGNACION: el backend crea el resultado y recién entonces
 * cambia el estado (ver AsignadorDonaciones.registrarDonacionPendienteDeAprobacion), así que esta
 * lista <em>es</em> el listado de donaciones pendientes de asignación, con sus propuestas.
 */
@Getter
@Setter
@NoArgsConstructor
public class ResultadoMatchmakingDTO {

    private DonacionDTO donacion;
    private List<PropuestaAsignacionDTO> propuestasOrdenadas = new ArrayList<>();

    /** true cuando la propuesta apareció en todos los algoritmos (ranking de intersección). */
    private boolean huboCoincidenciaTotal;

    public int cantidadPropuestas() {
        return propuestasOrdenadas == null ? 0 : propuestasOrdenadas.size();
    }
}
