package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.*;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanificadorRutasService {
    private static final Logger log = LoggerFactory.getLogger(PlanificadorRutasService.class);

    private final RepositorioRutas repoRutas;
    private final RepositorioChoferes repoChoferes;
    private final RepositorioItemEntrega repoItemEntrega;
    private final RepositorioCamiones repoCamiones;
    private final RepositorioParadas repoParadas;
    private final PlanificadorDeRutas planificadorDominio;
    private final MongoTemplate mongoTemplate;

    public PlanificadorRutasService(RepositorioRutas repoRutas,
                                    RepositorioChoferes repoChoferes,
                                    RepositorioItemEntrega repoItemEntrega,
                                    RepositorioCamiones repoCamiones,
                                    RepositorioParadas repoParadas,
                       PlanificadorDeRutas planificadorDominio,
                                    MongoTemplate mongoTemplate) {
        this.repoRutas = repoRutas;
        this.repoChoferes = repoChoferes;
        this.repoItemEntrega = repoItemEntrega;
        this.repoCamiones = repoCamiones;
        this.repoParadas = repoParadas;
        this.planificadorDominio = planificadorDominio;
        this.mongoTemplate = mongoTemplate;
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

        return planificadorDominio.procesarCallbackRutas(asignacion, camionesDb, itemsDb);
    }

    public List<Ruta> asignarChoferes(List<Ruta> rutas) {
        if (rutas == null || rutas.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> choferesEnRutasActivas = repoRutas
                .findByEstadoIn(List.of(EstadoRuta.PROGRAMADA, EstadoRuta.EN_CURSO))
                .stream()
                .map(Ruta::getCamionAsignado)
                .filter(Objects::nonNull)
                .map(Camion::getChofer)
                .filter(Objects::nonNull)
                .map(Chofer::getIdChofer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Chofer> choferesDisponibles = new ArrayList<>(
                repoChoferes.findAll()
                        .stream()
                        .filter(Chofer::isActivo)
                        .filter(Chofer::isDisponible)
                        .filter(chofer -> !choferesEnRutasActivas.contains(chofer.getIdChofer()))
                        .toList()
        );
        Random random = new Random();

        List<Ruta> rutasPendientes = new ArrayList<>(rutas);
        List<Ruta> rutasAsignadas = new ArrayList<>();

        while (!rutasPendientes.isEmpty() && !choferesDisponibles.isEmpty()) {
            Ruta rutaElegida = rutasPendientes.remove(random.nextInt(rutasPendientes.size()));
            Camion camion = rutaElegida.getCamionAsignado();

            if (camion == null) {
                continue;
            }

            Chofer candidato = choferesDisponibles.remove(random.nextInt(choferesDisponibles.size()));
            Chofer choferElegido = buscarChofer(candidato.getIdChofer());

            if (choferElegido == null) {
                // Otra réplica ya lo reclamó entre el findAll y el findAndModify.
                // Devolvemos la ruta y seguimos con el próximo chofer.
                rutasPendientes.add(rutaElegida);
                continue;
            }

            camion.setChofer(choferElegido);
            camion.ocupado();

            try {
                repoCamiones.save(camion);
                persistirRutaAsignada(rutaElegida);
            } catch (RuntimeException e) {
                // El claim ya quedó commiteado. Si falla el guardado posterior no queremos
                // dejar al chofer ocupado sin ruta: se libera y se propaga el error original.
                try {
                    liberarChofer(choferElegido.getIdChofer(), choferElegido.getVersion());
                } catch (RuntimeException liberacionError) {
                    e.addSuppressed(liberacionError);
                    log.error("No se pudo liberar al chofer {} tras fallar la asignación",
                            choferElegido.getIdChofer(), liberacionError);
                }
                throw e;
            }
            rutasAsignadas.add(rutaElegida);
        }

        if (rutasAsignadas.size() < rutas.size()) {
            log.warn("Se generaron {} rutas pero solo se asignaron {}. Las no asignadas (por falta de " +
                    "choferes disponibles o por no tener camión) no fueron persistidas y sus ítems " +
                    "continúan pendientes para futuras planificaciones.",
                    rutas.size(), rutasAsignadas.size());
        } else {
            log.info("Se asignaron y persistieron exitosamente {} rutas nuevas.", rutasAsignadas.size());
        }

        return rutasAsignadas;
    }

    private Chofer buscarChofer(UUID idChofer) {
        Query query = new Query(Criteria.where("_id").is(idChofer)
                .and("disponible").is(true)
                .and("activo").ne(false));
        Update update = new Update().set("disponible", false).inc("version", 1);
        return mongoTemplate.findAndModify(
                query, update, new FindAndModifyOptions().returnNew(true), Chofer.class);
    }

    private void liberarChofer(UUID idChofer, Long version) {
        // CAS por versión: sólo libera si nadie más tocó al chofer desde el claim.
        Query query = new Query(Criteria.where("_id").is(idChofer)
                .and("version").is(version)
                .and("activo").ne(false));
        Update update = new Update().set("disponible", true).inc("version", 1);
        mongoTemplate.updateFirst(query, update, Chofer.class);
    }

    private void persistirRutaAsignada(Ruta ruta) {
        if (ruta.getParadas() != null) {
            for (Parada parada : ruta.getParadas()) {
                repoParadas.save(parada);
                if (parada.getItems() != null) {
                    for (ItemEntrega item : parada.getItems()) {
                        repoItemEntrega.save(item);
                    }
                }
            }
        }
        // La ruta se guarda al final: si algo falla antes, no queda persistida una ruta
        // apuntando a un chofer que la compensación va a liberar.
        repoRutas.save(ruta);
    }
}
