package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CiudadJpaRepository extends JpaRepository<Ciudad, UUID> {
    Optional<Ciudad> findByNombreAndProvinciaNombre(String nombre, String provinciaNombre);
}
