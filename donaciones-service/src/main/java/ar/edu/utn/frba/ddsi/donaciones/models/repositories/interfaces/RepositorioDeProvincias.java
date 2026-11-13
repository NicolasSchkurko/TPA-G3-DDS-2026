package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioDeProvincias extends JpaRepository<Provincia, UUID> {
    Optional<Provincia> findByNombreAndPaisNombre(String nombre, String paisNombre);

    default Provincia guardar(Provincia provincia) { return save(provincia); }

    default Optional<Provincia> buscarPorNombreYPais(String nombre, String nombrePais) {
        return findByNombreAndPaisNombre(nombre, nombrePais);
    }

}
