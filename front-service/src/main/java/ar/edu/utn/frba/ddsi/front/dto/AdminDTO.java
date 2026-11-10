package ar.edu.utn.frba.ddsi.front.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Perfil de administrador vinculado a la cuenta (respuesta de donaciones-service). */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminDTO {
    private String id;
    private String nombreAMostrar;
    private String nombre;
    private String apellido;
    private String email;
    private MediosContactoDTO medioDeContacto;
}
