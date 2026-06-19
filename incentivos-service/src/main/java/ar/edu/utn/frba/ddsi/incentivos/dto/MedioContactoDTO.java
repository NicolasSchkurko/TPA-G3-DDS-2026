package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;

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
