package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Comando del formulario "Actualizar donación".
 *
 * <p>Es más chico que el de alta porque el PUT de donaciones-service sólo mira dos cosas: la
 * descripción y los bienes (ver DonacionService.actualizarDonacion). El donante, la entidad, la
 * subcategoría y la fecha de entrega no se tocan por esta vía — el donante y la fecha quedan
 * fijados en el alta, la entidad la define el asignador y la fecha de entrega la escribe el propio
 * backend al pasar a ENTREGADO. El estado se cambia aparte, con su motivo (PATCH .../estado).
 */
@Getter
@Setter
public class EdicionDonacionDTO {

    private String descripcion;

    private List<BienDonacionDTO> bienes = new ArrayList<>();

    public static EdicionDonacionDTO desde(DonacionDTO donacion) {
        EdicionDonacionDTO formulario = new EdicionDonacionDTO();
        if (donacion == null) {
            return formulario;
        }
        formulario.setDescripcion(donacion.getDescripcion());
        if (donacion.getBienes() != null) {
            formulario.setBienes(new ArrayList<>(donacion.getBienes()));
        }
        return formulario;
    }

    /**
     * Arma el body del PUT.
     *
     * @param incluirBienes false cuando la donación ya salió del depósito. En ese caso la clave
     *                      {@code bienes} no se manda: el backend sólo pisa lo que viene en el
     *                      body, y si la lista viaja con la donación fuera de depósito responde
     *                      400 ("No se pueden modificar los bienes...").
     */
    public Map<String, Object> aPayload(boolean incluirBienes) {
        if (descripcion != null) {
            descripcion = descripcion.trim();
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción de la donación no puede quedar vacía.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("descripcion", descripcion);

        if (incluirBienes) {
            bienes = BienDonacionDTO.normalizar(bienes);
            if (bienes.isEmpty()) {
                throw new IllegalArgumentException("La donación necesita al menos un bien.");
            }
            payload.put("bienes", bienes);
        }
        return payload;
    }
}
