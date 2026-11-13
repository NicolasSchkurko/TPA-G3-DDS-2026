package ar.edu.utn.frba.ddsi.donaciones.dto.identidad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Petición para vincular una cuenta de Keycloak con un perfil de dominio. */
@Getter
@Setter
public class VincularIdentidadRequest {

    /** Identificador inmutable de la cuenta en Keycloak (claim {@code sub}). */
    @NotBlank
    private String sub;

    /** DONANTE, ENTIDAD o ADMIN. */
    @NotBlank
    private String rol;

    @NotNull
    private UUID perfilId;
}
