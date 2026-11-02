package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio Spring Data JPA para Donante.
 * No se usa directamente desde los Gestores/Services: RepositorioDonantes actúa como
 * fachada, manteniendo los métodos públicos que ya existían cuando era un repositorio en memoria.
 */
public interface DonanteJpaRepository extends JpaRepository<Donante, UUID> {

  @Query("SELECT d FROM Donante d WHERE d.activo = true AND ("
      + " (TYPE(d.persona) = Humana AND ("
      + "   LOWER(TREAT(d.persona AS Humana).nombre) = LOWER(:nombre) OR "
      + "   LOWER(TREAT(d.persona AS Humana).apellido) = LOWER(:nombre) OR "
      + "   LOWER(CONCAT(TREAT(d.persona AS Humana).nombre, ' ', TREAT(d.persona AS Humana).apellido)) = LOWER(:nombre)"
      + " )) OR "
      + " (TYPE(d.persona) = Juridica AND LOWER(TREAT(d.persona AS Juridica).razonSocial) = LOWER(:nombre))"
      + ")")
  List<Donante> buscarPorNombreActivo(@Param("nombre") String nombre);

  Page<Donante> findByActivoTrue(Pageable pageable);
}
