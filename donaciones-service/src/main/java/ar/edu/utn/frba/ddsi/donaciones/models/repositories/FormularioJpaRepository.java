package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para Formulario.
 * No se usa directamente desde los Gestores/Services: RepositorioFormularios actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface FormularioJpaRepository extends JpaRepository<Formulario, UUID> {
}
