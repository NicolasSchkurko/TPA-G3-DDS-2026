package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinciaJpaRepository extends JpaRepository<Provincia, UUID> {
    Optional<Provincia> findByNombreAndPaisNombre(String nombre, String paisNombre);
}
