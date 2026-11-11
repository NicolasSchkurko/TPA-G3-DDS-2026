package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hook de reconciliación puntual: si el donante ya no existe o está inactivo en donaciones,
 * da de baja su perfil en incentivos. La outbox de donaciones sigue siendo el camino normal;
 * esto cubre el caso en que el front detecta el huérfano y quiere resolverlo al vuelo.
 */
@RestController
@RequestMapping("/api/reconciliacion")
public class ReconciliacionController {

    private static final Logger log = LoggerFactory.getLogger(ReconciliacionController.class);

    private final DonacionesClient donacionesClient;
    private final IncentivosClient incentivosClient;

    public ReconciliacionController(DonacionesClient donacionesClient,
                                    IncentivosClient incentivosClient) {
        this.donacionesClient = donacionesClient;
        this.incentivosClient = incentivosClient;
    }

    @PostMapping("/{idUsuario}")
    public ResponseEntity<Void> reconciliar(@PathVariable UUID idUsuario) {
        Optional<Boolean> activo = donacionesClient.donanteSigueActivo(idUsuario);

        if (activo.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (activo.get()) {
            return ResponseEntity.noContent().build();
        }

        try {
            incentivosClient.eliminarPerfil(idUsuario);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.warn("No se pudo reconciliar el perfil {} en incentivos: {}",
                    idUsuario, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
