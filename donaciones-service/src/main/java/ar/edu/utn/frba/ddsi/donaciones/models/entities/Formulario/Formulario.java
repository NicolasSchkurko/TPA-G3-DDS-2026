package ar.edu.utn.frba.ddsi.donaciones.models.entities.Formulario;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class Formulario {
    private PersonaDonante donante;
    private List<Donacion> donaciones;
    private LocalDate fechaRealizacion;

    public Formulario(PersonaDonante donante, List<Bien> bienes, LocalDate fechaRealizacion) {
        this.donante = donante;
        this.donaciones = SegmentadorDonaciones.segmentar(donante, bienes);
        this.fechaRealizacion = fechaRealizacion;
    }
}
