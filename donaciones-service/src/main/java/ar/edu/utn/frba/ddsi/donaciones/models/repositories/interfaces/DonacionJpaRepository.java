package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para Donacion.
 * No se usa directamente desde los Gestores/Services: RepositorioDonaciones actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface DonacionJpaRepository extends JpaRepository<Donacion, UUID> {
    List<Donacion> findByEstado(Estado estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Donacion d SET d.donante = null WHERE d.donante.id = :donanteId")
    void desvincularDonante(@Param("donanteId") UUID donanteId);
}
