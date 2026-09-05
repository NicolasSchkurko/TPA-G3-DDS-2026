package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioPaises {

    private final PaisJpaRepository jpaRepository;

    public RepositorioPaises(PaisJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public Pais guardar(Pais pais) {
        return jpaRepository.save(pais);
    }

    public Optional<Pais> buscarPorNombre(String nombre) {
        return jpaRepository.findByNombre(nombre);
    }
}
