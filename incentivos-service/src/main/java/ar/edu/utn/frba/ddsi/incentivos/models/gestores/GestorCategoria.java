/*
package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.events.CategoriaCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.UltimaMisionCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GestorCategoria {
    private final RepositorioCategorias repositorio;
    private final ApplicationEventPublisher eventPublisher;
    private final MisionFactory misionFactory;

    public GestorCategoria(RepositorioCategorias repositorio,
                           ApplicationEventPublisher eventPublisher,
                           MisionFactory misionFactory) {
        this.repositorio = repositorio;
        this.eventPublisher = eventPublisher;
        this.misionFactory = misionFactory;

    }

    @EventListener
    public void avanzarCategoria(UltimaMisionCategoria event) {
        if (event.idCategoriaCompletada() == null) {
            return;
        }

        Categoria categoriaAnterior = repositorio.buscarPorId(event.idCategoriaCompletada());
        Categoria categoriaSiguiente = repositorio.buscarPorPosicionSecuencia(categoriaAnterior.getPosicionSecuencia() + 1);
        if (categoriaSiguiente == null || categoriaSiguiente.primeraMision() == null) {
            return;
        }

        eventPublisher.publishEvent(
                new CategoriaCambiada(
                        categoriaAnterior,
                        categoriaSiguiente,
                        event.idPerfil()
                )
        );
    }
}
*/