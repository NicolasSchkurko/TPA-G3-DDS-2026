package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Direccion;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

@Getter
@Setter
public class Ciudad {
    private String nombre;
    private Provincia provincia;

    public Ciudad(String nombre, Provincia provincia){
        this.nombre = nombre;
        this.provincia = provincia;
    }
}