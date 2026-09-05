package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaisJpaRepository extends JpaRepository<Pais, UUID> {
    Optional<Pais> findByNombre(String nombre);
}
