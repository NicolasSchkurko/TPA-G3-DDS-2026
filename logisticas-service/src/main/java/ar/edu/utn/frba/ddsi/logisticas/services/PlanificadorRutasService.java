package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.camiones.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.choferes.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.items.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.rutas.RepositorioRutas;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlanificadorRutasService {
    private static final Logger log = LoggerFactory.getLogger(PlanificadorRutasService.class);

    private final RepositorioRutas repoRutas;
    private final RepositorioChoferes repoChoferes;
    private final RepositorioItemEntrega repoItemEntrega;
    private final RepositorioCamiones repoCamiones;
    private final RepositorioParadas repoParadas;
    private final PlanificadorDeRutas planificadorDominio;

    public PlanificadorRutasService(RepositorioRutas repoRutas,
                                    RepositorioChoferes repoChoferes,
                                    RepositorioItemEntrega repoItemEntrega,
                                    RepositorioCamiones repoCamiones,
                                    RepositorioParadas repoParadas,
                       PlanificadorDeRutas planificadorDominio) {
        this.repoRutas = repoRutas;
        this.repoChoferes = repoChoferes;
        this.repoItemEntrega = repoItemEntrega;
        this.repoCamiones = repoCamiones;
        this.repoParadas = repoParadas;
        this.planificadorDominio = planificadorDominio;
    }

    /**
     * Invocado por el Controller cuando llega el HTTP POST de callback desde el proveedor externo.
     */
    public List<Ruta> procesarCallbackRutas(String jsonAsignacion) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<UUID>> asignacion;
        try {
            asignacion = mapper.readValue(jsonAsignacion, new TypeReference<Map<String, List<UUID>>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("El formato del JSON recibido no es válido", e);
        }

        List<UUID> todosLosIdsItems = asignacion.values().stream()
                .flatMap(List::stream)
                .toList();

        List<Camion> camionesDb;
        List<ItemEntrega> itemsDb;

        try {
            camionesDb = repoCamiones.findAll();
            itemsDb = todosLosIdsItems.stream()
                    .map(id -> repoItemEntrega.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada con el ID: " + id)))
                    .toList();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e; // Se relanzan para que el Controller responda HTTP 400/422 en lugar de 500
        } catch (Exception e) {
            throw new RuntimeException("Falla en la base de datos al recuperar información para el ruteo", e);
        }

        List<Ruta> rutasGeneradas = planificadorDominio.procesarCallbackRutas(asignacion, camionesDb, itemsDb);
        try {
            for (Ruta ruta : rutasGeneradas) {

                repoRutas.saveAndFlush(ruta);

                for (Parada parada : ruta.getParadas()) {

                    parada.setRuta(ruta);
                    repoParadas.saveAndFlush(parada);

                    for (ItemEntrega item : parada.getItems()) {
                        item.setParada(parada);
                        repoItemEntrega.saveAndFlush(item);
                    }
                }
            }
            log.info("Se guardaron exitosamente {} rutas nuevas", rutasGeneradas.size());

        } catch (Exception e) {
            log.error("Error al persistir las nuevas rutas en la base de datos", e);
            throw new RuntimeException("Error al persistir las nuevas rutas en la base de datos", e);
        }

        return rutasGeneradas;
    }

    public List<Ruta> asignarChoferes(List<Ruta> rutas){
        List<Chofer> choferesDisponibles = new ArrayList<>(
                repoChoferes.findAll()
                        .stream()
                        .filter(Chofer::isDisponible)
                        .toList()
        );
        Random random = new Random();

        if(rutas.size() <= choferesDisponibles.size()){
            for (Ruta ruta : rutas) {
                Chofer choferElegido = choferesDisponibles.get(random.nextInt(choferesDisponibles.size()));

                Camion camion = ruta.getCamionAsignado();
                if (camion != null) {
                    camion.setChofer(choferElegido);
                    camion.ocupado();
                    choferElegido.ocupado();

                    repoCamiones.save(camion);
                    repoChoferes.save(choferElegido);
                }

                repoRutas.saveAndFlush(ruta);
                choferesDisponibles.remove(choferElegido);
            }
            return rutas;
        } else {
            List<Ruta> rutasAsignadas = new ArrayList<>();
            for (Chofer chofer : choferesDisponibles) {
                Ruta rutaElegida = rutas.get(random.nextInt(rutas.size()));

                Camion camion = rutaElegida.getCamionAsignado();
                if (camion != null) {
                    camion.setChofer(chofer);
                    camion.ocupado();
                    chofer.ocupado();

                    repoCamiones.save(camion);
                    repoChoferes.save(chofer);
                }

                repoRutas.saveAndFlush(rutaElegida);
                rutas.remove(rutaElegida);
                rutasAsignadas.add(rutaElegida);
            }
            return rutasAsignadas;
        }
    }
}
