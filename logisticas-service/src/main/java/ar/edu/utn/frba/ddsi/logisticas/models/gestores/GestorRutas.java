package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GestorRutas {

    private final RepositorioRutas repoRutas;
    private final RepositorioItemEntrega repoItemEntrega;

    public GestorRutas(RepositorioRutas repoRutas, RepositorioItemEntrega repoItemEntrega) {
        this.repoRutas = repoRutas;
        this.repoItemEntrega = repoItemEntrega;
    }

    public Ruta obtenerRutaActualizada(UUID idRuta) {
        Ruta ruta = repoRutas.findById(idRuta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la ruta con id " + idRuta));

        List<UUID> idsDonaciones = ruta.obtenerTodosLosItems()
                .stream()
                .map(ItemEntrega::getIdDonacion)
                .distinct()
                .toList();

        Map<UUID, ItemEntrega> itemsActualizados =
                repoItemEntrega.findAllById(idsDonaciones).stream().collect(Collectors.toMap(
                        ItemEntrega::getIdDonacion,
                        Function.identity()
                ));

        for (Parada parada : ruta.getParadas()) {
            List<ItemEntrega> itemsActualizadosDeParada =
                    parada.getItems().stream()
                            .map(ItemEntrega::getIdDonacion)
                            .map(itemsActualizados::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            parada.setItems(itemsActualizadosDeParada);
        }

        return ruta;
    }

    public void guardarItemsDeRuta(Ruta ruta) {
        if (ruta.getParadas() == null) {
            return;
        }
        for (Parada parada : ruta.getParadas()) {
            if (parada.getItems() != null) {
                parada.getItems().forEach(repoItemEntrega::save);
            }
        }
    }

    public void eliminarItemSiExiste(UUID idDonacion) {
        if (repoItemEntrega.existsById(idDonacion)) {
            repoItemEntrega.deleteById(idDonacion);
        }
    }

    public void sincronizarItem(Ruta ruta, ItemEntrega item) {
        if (ruta == null || ruta.getParadas() == null) {
            return;
        }

        for (Parada parada : ruta.getParadas()) {
            if (parada.getItems() == null) {
                continue;
            }
            for (ItemEntrega itemDeRuta : parada.getItems()) {
                if (item.getIdDonacion().equals(itemDeRuta.getIdDonacion())) {
                    itemDeRuta.setEstado(item.getEstado());
                    itemDeRuta.setFotoComprobante(item.getFotoComprobante());
                    itemDeRuta.setFechaCambioEstado(item.getFechaCambioEstado());
                    itemDeRuta.setEventos(item.getEventos() == null
                            ? new ArrayList<>()
                            : new ArrayList<>(item.getEventos()));
                }
            }
        }

        repoRutas.save(ruta);
    }
}
