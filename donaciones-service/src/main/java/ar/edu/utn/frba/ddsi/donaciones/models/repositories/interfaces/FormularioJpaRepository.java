package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para Formulario.
 * No se usa directamente desde los Gestores/Services: RepositorioFormularios actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface FormularioJpaRepository extends JpaRepository<Formulario, UUID> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Formulario f SET f.donante = null WHERE f.donante.id = :donanteId")
    void desvincularDonante(@Param("donanteId") UUID donanteId);
}
