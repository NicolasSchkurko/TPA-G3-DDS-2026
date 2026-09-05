package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioProvincias {

    private final ProvinciaJpaRepository jpaRepository;

    public RepositorioProvincias(ProvinciaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public Provincia guardar(Provincia provincia) {
        return jpaRepository.save(provincia);
    }

    public Optional<Provincia> buscarPorNombreYPais(String nombre, String nombrePais) {
        return jpaRepository.findByNombreAndPaisNombre(nombre, nombrePais);
    }
}
