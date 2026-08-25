package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter

public class MedioContactoDTO {
    //dto para recibir de donaciones el medioDeContacto del cliente
    private String medioDeContacto;
    private String direccionContacto;

    public MedioContactoDTO(String medioDeContacto,
                            String direccionContacto){
        this.medioDeContacto = medioDeContacto;
        this.direccionContacto = direccionContacto;
    }
}
