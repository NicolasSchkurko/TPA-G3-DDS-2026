package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.logistica.EventoProcesado;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.EventoProcesadoJpaRepository;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fachada sobre {@link EventoProcesadoJpaRepository} (Spring Data JPA) para la deduplicación
 * de eventos de logística (Punto 75).
 *
 * <p>La API es mínima: saber si un evento ya fue procesado y reclamarlo. El reclamo se hace
 * insertando la fila al <em>comienzo</em> de la transacción de procesamiento: si el proceso
 * falla, el rollback borra la fila y el retry puede reprocesar; si otra réplica intenta
 * reclamar el mismo evento, la clave primaria duplicada la saca de la carrera antes de que
 * procese. Así la verificación y la inserción son atómicas, sin ventana check-then-act.
 */
@Repository
public class RepositorioEventosProcesados {

    private final EventoProcesadoJpaRepository jpaRepository;
    private final EntityManager entityManager;

    public RepositorioEventosProcesados(EventoProcesadoJpaRepository jpaRepository,
                                        EntityManager entityManager) {
        this.jpaRepository = jpaRepository;
        this.entityManager = entityManager;
    }

    /**
     * Indica si el evento con el id indicado ya fue procesado con éxito en alguna
     * réplica del clúster.
     */
    public boolean existe(long idEvento) {
        return jpaRepository.existsById(idEvento);
    }

    /**
     * Inserta la fila de deduplicación del evento dentro de la transacción del llamador y
     * fuerza el INSERT. Debe invocarse al comienzo de la transacción de procesamiento: la
     * fila solo queda persistida si esa transacción commitea (o sea, si el evento termina
     * procesado con éxito).
     *
     * <p>El {@code flush()} es deliberado: sin él, la violación de clave primaria recién
     * aparecería al commit, cuando el llamador ya no puede distinguirla de un fallo del
     * procesamiento. Al forzar el INSERT, la carrera entre réplicas se detecta antes de
     * procesar nada.
     *
     * @throws org.springframework.dao.DataIntegrityViolationException si otra instancia ya
     *     insertó la fila del mismo evento (carrera de réplicas).
     */
    @Transactional
    public void reclamar(long idEvento) {
        entityManager.persist(new EventoProcesado(idEvento, LocalDateTime.now()));
        entityManager.flush();
    }
}
