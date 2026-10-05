package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje;

import lombok.Getter;

/**
 * Un medio de contacto del donante. Es un valor, no una entidad: se construye entero y no
 * se modifica, así que no tiene setters.
 */
@Getter
public class MedioContacto {

    private final String medioDeContacto;
    private final String direccionContacto;

    public MedioContacto(String medio, String direccion) {
        this.medioDeContacto = medio;
        this.direccionContacto = direccion;
    }
}
