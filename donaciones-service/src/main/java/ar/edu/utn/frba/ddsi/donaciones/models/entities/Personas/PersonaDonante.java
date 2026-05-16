package ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public abstract class PersonaDonante {

    private MediosDeContacto mediosDeContacto;
}
