package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para EntidadBeneficiaria.
 * No se usa directamente desde los Gestores/Services: RepositorioEntidadesBeneficiarias
 * actúa como fachada sobre esta interfaz, manteniendo los métodos públicos que ya existían
 * cuando el repositorio era en memoria (guardar, obtenerTodas, buscarPorId, actualizar, eliminarPorId).
 */
public interface EntidadBeneficiariaJpaRepository extends JpaRepository<EntidadBeneficiaria, UUID> {
    Page<EntidadBeneficiaria> findByActivaTrue(Pageable pageable);

    @Query("""
        SELECT DISTINCT e FROM EntidadBeneficiaria e
        JOIN e.personaJuridica p
        JOIN p.mediosDeContacto mc
        JOIN mc.listaMediosDeContacto m
        WHERE TYPE(m) = Mail
          AND LOWER(TREAT(m AS Mail).direccionMail) = LOWER(:email)
          AND e.activa = true
        """)
    List<EntidadBeneficiaria> buscarPorEmailContacto(@Param("email") String email);

    Optional<EntidadBeneficiaria> findByKeycloakSubAndActivaTrue(String keycloakSub);
}