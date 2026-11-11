package ar.edu.utn.frba.ddsi.donaciones.dto.identidad;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Identidad vinculada a un correo electrónico: perfiles de donante, entidad
 * y administrador que comparten ese mismo contacto.
 */
@Getter
@Setter
public class IdentidadDTO {
    private String email;
    private UUID entidadId;
    private UUID donanteId;
    private UUID adminId;
    private List<String> roles = new ArrayList<>();
    /** true si alguno de los perfiles encontrados tiene un sub de Keycloak asignado. */
    private boolean vinculada;

    public boolean tieneRol(String rol) {
        return rol != null && roles != null && roles.contains(rol.toUpperCase());
    }
}
