package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorAsignaciones {
    private RepositorioDonaciones repositorioDonaciones;

    public GestorAsignaciones(RepositorioDonaciones repositorioDonaciones){
        this.repositorioDonaciones=repositorioDonaciones;
    }

    public void asignarEntidad(UUID donacionId, EntidadBeneficiaria entidad) {
        try {
            repositorioDonaciones.asignarEntidad(donacionId, entidad);
            System.out.println("Entidad asignada con éxito a la donación: " + donacionId);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al asignar entidad: " + e.getMessage());
        }
    }

//    public List<ResultadoMatchmaking> asignarDonaciones(List<Donacion> donacionesNoAsignadas, List<EntidadBeneficiaria> entidades) {
//        DonacionFacade donacionFacade = new DonacionFacade(new SegmentadorDonaciones(),
//                new AsignadorDonaciones());
//
//        donacionFacade.ejecutarAsignador(donacionesNoAsignadas, entidades);
//        return donacionFacade.obtenerDonacionesPendientesDeAprobacion();
//    }
}
