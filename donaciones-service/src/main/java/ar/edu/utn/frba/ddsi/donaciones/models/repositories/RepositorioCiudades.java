package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCiudades {

    private final CiudadJpaRepository jpaRepository;

    public RepositorioCiudades(CiudadJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public Ciudad guardar(Ciudad ciudad) {
        return jpaRepository.save(ciudad);
    }

    public Optional<Ciudad> buscarPorNombreYProvincia(String nombre, String nombreProvincia) {
        return jpaRepository.findByNombreAndProvinciaNombre(nombre, nombreProvincia);
    }
}
