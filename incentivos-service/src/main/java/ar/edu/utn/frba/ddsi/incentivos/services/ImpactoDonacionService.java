package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;

import java.util.UUID;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ImpactoDonacionService {
    //cliente
    private final RepositorioDonaciones donaciones = RepositorioDonaciones.getInstance();

    private final RepositorioPerfiles perfiles = RepositorioPerfiles.getInstance();

    private final NotificacionesClient notificaciones;

    public ImpactoDonacionService(NotificacionesClient notificaciones) {
        this.notificaciones = notificaciones;
    }

    public Void procesarImpactoDonacion(ImpactoDonacionDTO dto) {
        ImpactoDonacion donacion = new ImpactoDonacion(
                // TODO
                dto.getCadaAtributo()
        );
        donaciones.agregarDonacion(donacion);

        Perfil perfilActualizado = this.impactarDonacion(donacion);
        //TODO tmb activar carga de metricas y actividadMensual

        notificacionesClient.enviarNotificacion(
                perfilActualizado.mapToDTO()
            );

        return null;
    }

    public Perfil impactarDonacion(ImpactoDonacion donacion) {
        Perfil perfilActualizado = perfiles.actualizarPerfil(donacion);

        return perfilActualizado;
    }
}
