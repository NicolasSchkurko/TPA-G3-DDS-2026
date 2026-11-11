package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDeCiudades extends JpaRepository<Ciudad, UUID> {
    Optional<Ciudad> findByNombreAndProvinciaNombre(String nombre, String provinciaNombre);

    default Ciudad guardar(Ciudad ciudad) { return save(ciudad); }

    default Optional<Ciudad> buscarPorNombreYProvincia(String nombre, String nombreProvincia) {
        return findByNombreAndProvinciaNombre(nombre, nombreProvincia);
    }

}
