package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.Ranking;
import ar.edu.utn.frba.ddsi.incentivos.models.events.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
public class GestorPerfiles {
    private final RepositorioPerfiles repositorio;
    private final DonacionClient donacionClient;
    private final ApplicationEventPublisher eventPublisher;

    public GestorPerfiles(RepositorioPerfiles repositorio, ApplicationEventPublisher eventPublisher, DonacionClient donacionClient) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
        this.donacionClient = donacionClient;
    }



    public Perfil progresarPerfil(UUID idUsuario, ImpactoDonacion donacion) {
        Perfil perfil = repositorio.buscarPorIDUsuario(idUsuario);
        if (perfil == null) return null;

        ProgresoMision misionAnterior = perfil.getProgresoMisionActual();
        Categoria categoriaActual = perfil.getCategoriaActual();
        boolean misionCompletada = perfil.progresarMision(donacion);
        repositorio.actualizar(perfil);
        MedioContacto contacto = donacionClient.obtenerContactoPersona(idUsuario);

        if (misionCompletada) {
            if (categoriaActual.esUltimaMision(misionAnterior.getMision())) {
                eventPublisher.publishEvent(
                        new UltimaMisionCategoria(
                                categoriaActual.getIdCategoria(),
                                perfil.getIdPerfil()
                        )
                );
            } else {
                Mision misionNueva = categoriaActual.siguienteMision(misionAnterior.getMision());
                perfil.setProgresoMisionActual(new ProgresoMision(misionNueva));
                repositorio.actualizar(perfil);
                eventPublisher.publishEvent(
                        new MisionCambiada(misionAnterior.getMision().getNombreMision(),
                                misionAnterior.getMision().getInsigniaObjetivo().getNombre(),
                                perfil.getNombreUsuario(), contacto,
                                perfil.getProgresoMisionActual().getMision().getNombreMision()
                        )
                );
            }
        }
        return perfil;
    }


    @EventListener
    public void actualizarPerfil(CategoriaCambiada event) {
        if (event.categoriaNueva() == null) {
            return;
        }

        Perfil perfil = repositorio.buscarPorIDPerfil(event.idPerfil());

        ProgresoMision misionAnterior = perfil.getProgresoMisionActual();
        perfil.setCategoriaActual(event.categoriaNueva());
        perfil.setProgresoMisionActual(new ProgresoMision(event.categoriaNueva().primeraMision()));
        repositorio.actualizar(perfil);
        MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
        eventPublisher.publishEvent(
                new MisionCambiada(misionAnterior.getMision().getNombreMision(),
                        misionAnterior.getMision().getInsigniaObjetivo().getNombre(),
                        perfil.getNombreUsuario(),
                        contacto,
                        perfil.getProgresoMisionActual().getMision().getNombreMision()
                )
        );

        eventPublisher.publishEvent(
                new CategoriaNuevaPublicar(
                        event.categoriaAnterior().getNombre(),
                        perfil.getCategoriaActual().getNombre(),
                        perfil.getNombreUsuario(),
                        contacto
                )
        );
    }
}
