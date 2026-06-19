package ar.edu.utn.frba.ddsi.incentivos.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter

public class InsigniaDTO {
    private String nombre;
    private String descripcion;
    private String urlImagen;
    private LocalDate fechaObtencion;

    public InsigniaDTO(String nombre, String descripcion, String urlImagen, LocalDate fechaObtencion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
        this.fechaObtencion = fechaObtencion;
    }
}