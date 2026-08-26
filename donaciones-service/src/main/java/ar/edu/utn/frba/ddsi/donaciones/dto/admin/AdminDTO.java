package ar.edu.utn.frba.ddsi.donaciones.dto.admin;


import java.util.UUID;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDTO {
    private UUID id;

    // Campos comunes o de retorno
    private String nombreAMostrar;

    private String nombre;
    private String apellido;
    private int edad;
    private int numeroDeDocumento;
    private String genero; // "HOMBRE", "MUJER", "OTRO"

    private MediosContactoDTO medioDeContacto;
}
