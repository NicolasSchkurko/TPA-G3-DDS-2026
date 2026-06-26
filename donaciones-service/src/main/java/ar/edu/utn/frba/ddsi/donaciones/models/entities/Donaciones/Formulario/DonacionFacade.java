package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class DonacionFacade {
    private List<Donacion> donaciones;
    private SegmentadorDonaciones segmentador;
    private AsignadorDonaciones asignador;

    public List<Donacion> crearDonaciones(Formulario formulario){
        List<Donacion> donaciones = segmentador.segmentar(formulario.getDonante(), formulario.getDonaciones());
        for (Donacion donacion : donaciones) {
            if (donacion.getEstado() == null) {
                donacion.actualizarEstado(Estado.EN_DEPOSITO, "Ingreso por segmentación de formulario");
            }
        }
        return donaciones;
    }

    public void ejecutarAsignador(List<Donacion> donacionesNoAsignadas, List<EntidadBeneficiaria> entidades){
        asignador.ejecutarMatchmakingBatch(donacionesNoAsignadas, entidades);
    }
}
