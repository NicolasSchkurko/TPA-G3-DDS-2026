package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.ActualizacionEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCicloEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorRegistroEntregas;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EntregaService {

    private final RepositorioItemEntrega repoItemEntrega;
    private final GestorRegistroEntregas gestorRegistroEntregas;
    private final GestorCicloEntrega gestorCicloEntrega;

    public EntregaService(RepositorioItemEntrega repoItemEntrega,
                          GestorRegistroEntregas gestorRegistroEntregas,
                          GestorCicloEntrega gestorCicloEntrega) {
        this.repoItemEntrega = repoItemEntrega;
        this.gestorRegistroEntregas = gestorRegistroEntregas;
        this.gestorCicloEntrega = gestorCicloEntrega;
    }

    public Page<BienDTO> findAll(Pageable pageable) {
        return repoItemEntrega.findAll(pageable).map(item -> new BienDTO().aplicar(item));
    }

    public BienDTO findById(UUID id) {
        Optional<ItemEntrega> item = repoItemEntrega.findById(id);
        if (item.isEmpty()) {
            throw new RecursoNoEncontradoException("Entrega no encontrada");
        }
        return new BienDTO().aplicar(item.get());
    }

    public void delete(UUID id) {
        if (!repoItemEntrega.existsById(id)) {
            throw new RecursoNoEncontradoException("Entrega no encontrada");
        }
        repoItemEntrega.deleteById(id);
    }

    public void procesarPeticion(EntregaDTO request) {
        gestorRegistroEntregas.registrar(request);
    }

    public void actualizarEstado(UUID idDonacion, ActualizacionEntregaDTO request) {
        if (request == null || request.getEstado() == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }

        switch (request.getEstado().toUpperCase()) {
            case "ENTREGADA" -> gestorCicloEntrega.confirmar(idDonacion, request.getFotoUrl());
            case "NO_RECIBIDA" -> gestorCicloEntrega.marcarFallida(idDonacion, request.getJustificacion());
            case "PENDIENTE" -> gestorCicloEntrega.reingresar(idDonacion);
            default -> throw new IllegalArgumentException(
                    "Estado no válido. Use ENTREGADA, NO_RECIBIDA o PENDIENTE.");
        }
    }

    public BienesDTO obtenerEntregasNoRecibidas() {
        List<ItemEntrega> items = repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA);
        return new BienesDTO().aplicar(items);
    }
}
