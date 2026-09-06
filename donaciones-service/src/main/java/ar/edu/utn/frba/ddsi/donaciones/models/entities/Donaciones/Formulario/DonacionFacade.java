package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class DonacionFacade {
    private SegmentadorDonaciones segmentador;

    public DonacionFacade(SegmentadorDonaciones segmentador){
        this.segmentador = segmentador;
    }

    public List<Donacion> crearDonaciones(Formulario formulario){
        List<Donacion> donaciones = segmentador.segmentar(formulario.getDonante(), formulario.getDonaciones());
        for (Donacion donacion : donaciones) {
            if (donacion.getEstado() == null) {
                donacion.actualizarEstado(Estado.EN_DEPOSITO, "Ingreso por segmentación de formulario");
            }
        }
        return donaciones;
    }
}
