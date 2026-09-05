package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para EntidadBeneficiaria.
 * No se usa directamente desde los Gestores/Services: RepositorioEntidadesBeneficiarias
 * actúa como fachada sobre esta interfaz, manteniendo los métodos públicos que ya existían
 * cuando el repositorio era en memoria (guardar, obtenerTodas, buscarPorId, actualizar, eliminarPorId).
 */
public interface EntidadBeneficiariaJpaRepository extends JpaRepository<EntidadBeneficiaria, UUID> {
}