package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDePaises extends JpaRepository<Pais, UUID> {
    Optional<Pais> findByNombre(String nombre);

    default Pais guardar(Pais pais) { return save(pais); }

    default Optional<Pais> buscarPorNombre(String nombre) { return findByNombre(nombre); }

    default Pais obtenerOCrearPais(String nombre) {
        return buscarPorNombre(nombre).orElseGet(() -> guardar(new Pais(nombre)));
    }

}
