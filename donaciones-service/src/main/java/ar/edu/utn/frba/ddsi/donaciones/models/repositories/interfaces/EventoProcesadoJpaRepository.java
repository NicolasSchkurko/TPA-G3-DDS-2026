package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.logistica.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistencia JPA de {@link EventoProcesado}. La tabla de deduplicación de eventos de
 * logística: clave primaria {@code id_evento} ({@code Long}).
 */
public interface EventoProcesadoJpaRepository extends JpaRepository<EventoProcesado, Long> {
}
