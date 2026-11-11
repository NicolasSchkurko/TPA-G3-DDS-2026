package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class GestorCicloEntrega {

    private final RepositorioItemEntrega repoItemEntrega;
    private final RepositorioRutas repoRutas;
    private final GestorPublicacionEventos gestorPublicacionEventos;
    private final GestorRutas gestorRutas;

    public GestorCicloEntrega(RepositorioItemEntrega repoItemEntrega,
                              RepositorioRutas repoRutas,
                              GestorPublicacionEventos gestorPublicacionEventos,
                              GestorRutas gestorRutas) {
        this.repoItemEntrega = repoItemEntrega;
        this.repoRutas = repoRutas;
        this.gestorPublicacionEventos = gestorPublicacionEventos;
        this.gestorRutas = gestorRutas;
    }

    @Transactional
    public ItemEntrega confirmar(UUID idDonacion, String foto) {
        if (faltaTexto(foto)) {
            throw new IllegalArgumentException("Se requiere una foto para confirmar la entrega exitosa.");
        }

        ItemEntrega item = buscarItem(idDonacion);
        Ruta ruta = buscarRuta(idDonacion);
        ItemEntrega actualizado = gestorPublicacionEventos.publicarEntregaConfirmada(item, ruta, foto);
        repoItemEntrega.save(actualizado);
        gestorRutas.sincronizarItem(ruta, actualizado);
        return actualizado;
    }

    @Transactional
    public ItemEntrega marcarFallida(UUID idDonacion, String justificacion) {
        if (faltaTexto(justificacion)) {
            throw new IllegalArgumentException("Se requiere justificar el motivo por el cual falló la entrega.");
        }

        ItemEntrega item = buscarItem(idDonacion);
        Ruta ruta = buscarRuta(idDonacion);
        ItemEntrega actualizado = gestorPublicacionEventos.publicarEntregaFallida(item, ruta, justificacion);
        repoItemEntrega.save(actualizado);
        gestorRutas.sincronizarItem(ruta, actualizado);
        return actualizado;
    }

    @Transactional
    public ItemEntrega reingresar(UUID idDonacion) {
        ItemEntrega item = buscarItem(idDonacion);
        if (item.getEstado() != EstadoEntrega.NO_RECIBIDA) {
            throw new IllegalStateException("Solo se puede reingresar a depósito una entrega en estado NO_RECIBIDA (estado actual: "
                    + item.getEstado() + ").");
        }

        ItemEntrega actualizado = gestorPublicacionEventos.publicarReingresoDeposito(item);
        repoItemEntrega.save(actualizado);
        repoRutas.findFirstByParadasItemsIdDonacion(idDonacion)
                .ifPresent(ruta -> gestorRutas.sincronizarItem(ruta, actualizado));
        return actualizado;
    }

    private ItemEntrega buscarItem(UUID idDonacion) {
        return repoItemEntrega.findById(idDonacion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Donación no encontrada con el ID proporcionado"));
    }

    private Ruta buscarRuta(UUID idDonacion) {
        return repoRutas.findFirstByParadasItemsIdDonacion(idDonacion)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró la ruta correspondiente a la donación " + idDonacion));
    }

    private boolean faltaTexto(String elemento) {
        return elemento == null || elemento.trim().isEmpty();
    }
}
