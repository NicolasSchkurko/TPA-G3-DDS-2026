package ar.edu.utn.frba.ddsi.donaciones.models.sheduler;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsignacionScheduler {

    private final GestorDonaciones gestorDonaciones;
    private final GestorEntidadesBeneficiarias gestorEntidades;
    private final GestorMatchmaking gestorMatchmaking;

    public AsignacionScheduler(GestorDonaciones gestorDonaciones, GestorEntidadesBeneficiarias gestorEntidades, GestorMatchmaking gestorMatchmaking) {
        this.gestorDonaciones = gestorDonaciones;
        this.gestorEntidades = gestorEntidades;
        this.gestorMatchmaking = gestorMatchmaking;
    }

    @Scheduled(cron = "0 0 18,0,2,4,6,8 * * *")
    public void ejecutarAsignacion() {
        AsignadorDonaciones asignadorDonaciones = new AsignadorDonaciones(gestorMatchmaking,gestorDonaciones);
        List<Donacion> donacionesNoAsignadas = gestorDonaciones.listarSinAsignacion();
        List<EntidadBeneficiaria> entidades = gestorEntidades.listarTodasLasEntidades();
        asignadorDonaciones.ejecutarMatchmakingBatch(donacionesNoAsignadas,entidades);
    }
}
