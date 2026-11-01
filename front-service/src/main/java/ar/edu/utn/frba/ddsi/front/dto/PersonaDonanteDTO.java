package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDonanteDTO {
    private UUID id;
    private String nombre;
    private String apellido;
    private String nombreDeUsuario;
    private String correoElectronico;
    private String telefono;
}
