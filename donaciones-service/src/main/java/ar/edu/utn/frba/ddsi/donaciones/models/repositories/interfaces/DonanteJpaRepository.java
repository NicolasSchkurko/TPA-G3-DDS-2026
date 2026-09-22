package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para Donante.
 * No se usa directamente desde los Gestores/Services: RepositorioDonantes actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface DonanteJpaRepository extends JpaRepository<Donante, UUID> {
}
