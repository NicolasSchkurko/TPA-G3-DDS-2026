package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para Administrador.
 * No se usa directamente desde los Gestores/Services: RepositorioAdministradores actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface AdministradorJpaRepository extends JpaRepository<Administrador, UUID> {
}
