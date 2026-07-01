package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Administrador {
    private UUID id;

    // Campos comunes o de retorno
    private String nombreAMostrar;

    private String nombre;
    private String apellido;
    private int edad;
    private int numeroDeDocumento;
    private String genero; // "HOMBRE", "MUJER", "OTRO"

    private MediosDeContacto medioDeContacto;

    public Administrador(String nomMostrar, String nom, String apellido, int edad,
                         int numDoc, String genero, MedioDeContacto medioDeContacto) {
        this.id = UUID.randomUUID();
        this.medioDeContacto.agregarMedioDeContacto(medioDeContacto);
        this.nombre = nom;
        this.apellido = apellido;
        this.edad = edad;
        this.numeroDeDocumento = numDoc;
    }

    public String darNombre() {
        return nombre + " " + apellido;
    }
}
