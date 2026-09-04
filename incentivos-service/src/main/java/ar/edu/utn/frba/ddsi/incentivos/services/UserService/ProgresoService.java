package ar.edu.utn.frba.ddsi.incentivos.services.UserService;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.*;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioActividades;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioRankings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class ProgresoService {
    private final RepositorioPerfiles repoPerfiles;
    private final DonacionClient donacionClient;
    private final RepositorioActividades repoActividades;
    private final RepositorioRankings repoRankings;
    private final RepositorioCategorias repoCategorias;
    private final ApplicationEventPublisher eventPublisher;

    public ProgresoService(ApplicationEventPublisher eventPublisher, RepositorioPerfiles repositorio,
                           DonacionClient donacionClient, RepositorioCategorias repoCategorias,
                       RepositorioRankings rankings, RepositorioActividades repoActividades) {
        this.repoPerfiles = repositorio;
        this.donacionClient = donacionClient;
        this.repoRankings = rankings;
        this.repoActividades= repoActividades;
        this.repoCategorias = repoCategorias;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    private void guardarDonacion(MisionCompletada event) {
        Perfil perfil = event.perfilActualizado();
        ImpactoDonacion impactoDonacion = event.impactoDonacion();

        if (perfil == null || impactoDonacion == null) {
            return;
        }

        HistorialActividad actividad =
                repoActividades
                        .findByIdPerfil(perfil.getIdPerfil())
                        .orElseGet(() ->
                                new HistorialActividad(
                                        perfil.getIdPerfil(),
                                        new ArrayList<>()
                                ));
        actividad.agregarDonacion(impactoDonacion);
        repoActividades.save(actividad);
    }

    @EventListener
    public void avanzarCategoria(UltimaMisionCategoria event) {
        if (event.perfil() == null) {
            return;
        }

        Perfil p = event.perfil();

        ProgresoMision misionAnterior = p.getProgresoMisionActual();
        Categoria categoriaAnterior = repoCategorias.buscarPorId(p.getCategoriaActual().getIdCategoria());
        Categoria categoriaSiguiente = repoCategorias.buscarPorPosicionSecuencia(categoriaAnterior.getPosicionSecuencia() + 1);
        if (categoriaSiguiente == null || categoriaSiguiente.primeraMision() == null) {
            return;
        }

        p.setCategoriaActual(categoriaSiguiente);
        p.setProgresoMisionActual(new ProgresoMision(categoriaSiguiente.primeraMision()));

        repoPerfiles.actualizar(p);

        MedioContacto contacto = donacionClient.obtenerContactoPersona(p.getIdUsuario());

        //activar notificarCambioCategoria en NotificacionClient
        eventPublisher.publishEvent(
                new CategoriaNuevaPublicar(
                        categoriaAnterior.getNombre(),
                        p.getCategoriaActual().getNombre(),
                        p.getNombreUsuario(),
                        contacto
                )
        );

        //activar notificarCambioMision en NotificacionClient
        //activa publicarInsignia en n8nClient
        eventPublisher.publishEvent(
                new MisionCambiada(
                        misionAnterior.getMision().getNombreMision(),
                        misionAnterior.getMision().getInsigniaObjetivo().getNombre(),
                        p.getNombreUsuario(), contacto,
                        p.getProgresoMisionActual().getMision().getNombreMision()
                )
        );
    }

    @EventListener
    public void avanzarMision(MisionCompletada event) {
        Perfil perfil = event.perfilActualizado();

        if (perfil == null) {
            return;
        }

        Mision misionAnterior = perfil.getProgresoMisionActual().getMision();
        Categoria categoria = repoCategorias.buscarPorId(perfil.getCategoriaActual().getIdCategoria());

        perfil.setProgresoMisionActual(
                new ProgresoMision(categoria.siguienteMision(misionAnterior))
        );

        repoPerfiles.actualizar(perfil);

        MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());

        //activar notificarCambioMision en NotificacionClient
        //activa publicarInsignia en n8nClient
        eventPublisher.publishEvent(
                new MisionCambiada(
                        misionAnterior.getNombreMision(),
                        misionAnterior.getInsigniaObjetivo().getNombre(),
                        perfil.getNombreUsuario(), contacto,
                        perfil.getProgresoMisionActual().getMision().getNombreMision()
                )
        );
    }
}
