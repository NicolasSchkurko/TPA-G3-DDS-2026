package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AsignacionService {

    private final AsignadorDonaciones asignador;
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioEntidadesBeneficiarias repositorioEntidades;

    public AsignacionService(AsignadorDonaciones asignador,
                             RepositorioDonaciones repositorioDonaciones,
                             RepositorioEntidadesBeneficiarias repositorioEntidades) {
        this.asignador = asignador;
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioEntidades = repositorioEntidades;
    }

    public void ejecutarAsignacion() {
        List<Donacion> donacionesNoAsignadas = repositorioDonaciones.findPendient();
        List<EntidadBeneficiaria> entidades = repositorioEntidades.findAll();
        asignador.ejecutarMatchmakingBatch(donacionesNoAsignadas, entidades);
    }
}
