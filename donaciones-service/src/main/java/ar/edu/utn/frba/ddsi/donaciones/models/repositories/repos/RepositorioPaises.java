package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.PaisJpaRepository;

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

    // Antes vivía en GestorDirecciones: busca el Pais por nombre; si no existe, lo crea
    // y lo persiste, para no duplicar el catálogo geográfico compartido.
    public Pais obtenerOCrearPais(String nombre) {
        return buscarPorNombre(nombre).orElseGet(() -> guardar(new Pais(nombre)));
    }
}
