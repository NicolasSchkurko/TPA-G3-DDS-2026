package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.LogisticaClient;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.GestorNotificacionesEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntregasService {
    private final RepositorioDonaciones repositorio;
    private final RepositorioEntidadesBeneficiarias repositorioEntidades;
    private final GestorNotificacionesEventos gestorNotificaciones;
    private final LogisticaClient logisticaClient;
    private final RepositorioRutas repositorioRutas;

    public EntregasService(RepositorioDonaciones repositorio,
                           RepositorioEntidadesBeneficiarias repositorioEntidades,
                           GestorNotificacionesEventos gestorNotificaciones,
                           LogisticaClient logistica,
                           RepositorioRutas repoRutas) {
        this.repositorio = repositorio;
        this.repositorioEntidades = repositorioEntidades;
        this.gestorNotificaciones = gestorNotificaciones;
        this.logisticaClient = logistica;
        this.repositorioRutas = repoRutas;
    }

    public void planificarRutas(){
        List<Donacion> donacionesEntregar = repositorio.findEntregarPendient();

    }
}
