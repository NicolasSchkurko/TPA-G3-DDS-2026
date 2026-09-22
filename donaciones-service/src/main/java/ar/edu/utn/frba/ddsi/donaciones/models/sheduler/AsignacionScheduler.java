package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAsignaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsignacionScheduler {

    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias;
    private final GestorMatchmaking gestorMatchmaking;
    private final GestorAsignaciones gestorAsignaciones;
    private final RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking;

    public AsignacionScheduler(RepositorioDonaciones repositorioDonaciones, RepositorioEntidadesBeneficiarias repositorioEntidadesBeneficiarias, GestorMatchmaking gestorMatchmaking,
                               GestorAsignaciones gestorAsignaciones, RepositorioDeResultadosMatchmaking repositorioDeResultadosMatchmaking) {
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioEntidadesBeneficiarias = repositorioEntidadesBeneficiarias;
        this.gestorMatchmaking = gestorMatchmaking;
        this.gestorAsignaciones = gestorAsignaciones;
        this.repositorioDeResultadosMatchmaking = repositorioDeResultadosMatchmaking;
    }

    @Scheduled(cron = "0 0 18,0,2,4,6,8 * * *")
    public void ejecutarAsignacion() {
        AsignadorDonaciones asignadorDonaciones = new AsignadorDonaciones(gestorMatchmaking,gestorAsignaciones,repositorioDeResultadosMatchmaking);
        List<Donacion> donacionesNoAsignadas = repositorioDonaciones.buscarDonacionesSinAsignar();
        List<EntidadBeneficiaria> entidades = repositorioEntidadesBeneficiarias.obtenerTodas();
        asignadorDonaciones.ejecutarMatchmakingBatch(donacionesNoAsignadas,entidades);
    }
}
