package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedioContacto {
    private String medioDeContacto;
    private String direccionContacto;

    public MedioContacto(String medio, String direc){
        this.direccionContacto = direc;
        this.medioDeContacto = medio;
    }
}
