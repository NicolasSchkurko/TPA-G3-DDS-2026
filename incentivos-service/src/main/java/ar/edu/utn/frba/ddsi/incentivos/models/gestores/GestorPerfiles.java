package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GestorPerfiles {
    private final ApplicationEventPublisher eventPublisher;

    public GestorPerfiles(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public Boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
        ProgresoMision misionAnterior = perfil.getProgresoMisionActual();
        Boolean misionCompletada = perfil.progresarMision(donacion); //perfil cambio, pero no la mision ni categoria

        if (misionCompletada) {
            //activa guardarDonacion para actualizar actividad del perfil en progresoService
            eventPublisher.publishEvent(
                    new MisionCompletada(
                            donacion,
                            perfil
                    )
            );
            //activa avanzarCategoria para actualizar categoria y mision del perfil en progresoService
            if (perfil.getCategoriaActual().esUltimaMision(misionAnterior.getMision())) {
                eventPublisher.publishEvent(
                        new UltimaMisionCategoria(
                                perfil
                        )
                );
            } else { //son asincronicos los publisher, asi q hay q repetir logica
                //activa avanzarMision para actualizar mision del perfil en progresoService
                eventPublisher.publishEvent(
                        new MisionCompletada(
                                null,
                                perfil
                        )
                );
            }
        }

        return misionCompletada;
    }

    public Perfil actualizar(Perfil perfilModificado) {
        if (perfilModificado == null || perfilModificado.getIdUsuario() == null) {
            return null;
        }

        Perfil existente = this.buscarPorIDUsuario(perfilModificado.getIdUsuario());
        if (existente != null) {
            // Actualizar solo los campos no nulos del perfilModificado
            if (perfilModificado.getNombreUsuario() != null) {
                existente.setNombreUsuario(perfilModificado.getNombreUsuario());
            }
            if (perfilModificado.getCategoriaActual() != null) {
                existente.setCategoriaActual(perfilModificado.getCategoriaActual());
            }
            if (perfilModificado.getInsignias() != null) {
                existente.setInsignias(perfilModificado.getInsignias());
            }
            if (perfilModificado.getMisionActual() != null) {
                existente.setMisionActual(perfilModificado.getMisionActual());
            }
            if (perfilModificado.getPosicionRanking() != null) {
                existente.setPosicionRanking(perfilModificado.getPosicionRanking());
            }

            int index = perfiles.indexOf(existente);
            if (index >= 0) {
                perfiles.set(index, existente);
            }
        }
        return existente;
    }
}
