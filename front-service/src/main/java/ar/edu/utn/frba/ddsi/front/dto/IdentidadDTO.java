package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Perfiles de dominio vinculados a un email (respuesta de donaciones-service). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdentidadDTO {
    private String email;
    private UUID entidadId;
    private UUID donanteId;
    private UUID adminId;
    private List<String> roles = new ArrayList<>();

    public boolean tieneRol(String rol) {
        return rol != null && roles != null && roles.stream().anyMatch(r -> r.equalsIgnoreCase(rol));
    }
}
