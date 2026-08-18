package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class Formulario {
    private Donante donante;
    private List<Bien> donaciones;
    private LocalDate fechaRealizacion;

    public Formulario(Donante donante, List<Bien> bienes, LocalDate fechaRealizacion) {
        this.donante = donante;
        this.donaciones = bienes;
        this.fechaRealizacion = fechaRealizacion;
    }
}
