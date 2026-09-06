package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.ProvinciaJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioProvincias {

    private final ProvinciaJpaRepository jpaRepository;
    private final RepositorioPaises repositorioPaises;

    public RepositorioProvincias(ProvinciaJpaRepository jpaRepository, RepositorioPaises repositorioPaises) {
        this.jpaRepository = jpaRepository;
        this.repositorioPaises = repositorioPaises;
    }

    public Provincia guardar(Provincia provincia) {
        return jpaRepository.save(provincia);
    }

    public Optional<Provincia> buscarPorNombreYPais(String nombre, String nombrePais) {
        return jpaRepository.findByNombreAndPaisNombre(nombre, nombrePais);
    }

    // Antes vivía en GestorDirecciones: busca la Provincia por nombre (dentro de su Pais);
    // si algún nivel no existe todavía, lo crea y lo persiste.
    public Provincia obtenerOCrearProvincia(String nombreProvincia, String nombrePais) {
        return buscarPorNombreYPais(nombreProvincia, nombrePais)
                .orElseGet(() -> {
                    Pais pais = repositorioPaises.obtenerOCrearPais(nombrePais);
                    return guardar(new Provincia(nombreProvincia, pais));
                });
    }
}
