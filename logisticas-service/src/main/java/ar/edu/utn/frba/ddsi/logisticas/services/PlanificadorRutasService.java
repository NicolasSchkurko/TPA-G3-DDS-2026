package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PlanificadorDeRutas.PlanificadorDeRutas;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorAsignacionChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PlanificadorRutasService {

    private final RepositorioItemEntrega repoItemEntrega;
    private final RepositorioCamiones repoCamiones;
    private final PlanificadorDeRutas planificadorDominio;
    private final GestorAsignacionChoferes gestorAsignacionChoferes;

    public PlanificadorRutasService(RepositorioItemEntrega repoItemEntrega,
                                    RepositorioCamiones repoCamiones,
                                    PlanificadorDeRutas planificadorDominio,
                                    GestorAsignacionChoferes gestorAsignacionChoferes) {
        this.repoItemEntrega = repoItemEntrega;
        this.repoCamiones = repoCamiones;
        this.planificadorDominio = planificadorDominio;
        this.gestorAsignacionChoferes = gestorAsignacionChoferes;
    }

    public List<Ruta> procesarCallbackRutas(String jsonAsignacion) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<UUID>> asignacion;
        try {
            asignacion = mapper.readValue(jsonAsignacion, new TypeReference<Map<String, List<UUID>>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("El formato del JSON recibido no es válido", e);
        }

        List<UUID> idsItems = asignacion.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        List<Camion> camionesDb;
        List<ItemEntrega> itemsDb;

        try {
            camionesDb = repoCamiones.findAllById(asignacion.keySet());
            itemsDb = repoItemEntrega.findAllById(idsItems);

            Set<UUID> encontrados = itemsDb.stream()
                    .map(ItemEntrega::getIdDonacion)
                    .collect(Collectors.toSet());
            UUID faltante = idsItems.stream()
                    .filter(id -> !encontrados.contains(id))
                    .findFirst()
                    .orElse(null);
            if (faltante != null) {
                throw new IllegalArgumentException("Entrega no encontrada con el ID: " + faltante);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falla en la base de datos al recuperar información para el ruteo", e);
        }

        return planificadorDominio.procesarCallbackRutas(asignacion, camionesDb, itemsDb);
    }

    public List<Ruta> asignarChoferes(List<Ruta> rutas) {
        return gestorAsignacionChoferes.asignarChoferes(rutas);
    }
}
