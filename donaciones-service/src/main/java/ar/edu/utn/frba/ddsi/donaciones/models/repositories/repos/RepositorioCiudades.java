package ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.CiudadJpaRepository;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioCiudades {

    private final CiudadJpaRepository jpaRepository;
    private final RepositorioProvincias repositorioProvincias;

    public RepositorioCiudades(CiudadJpaRepository jpaRepository, RepositorioProvincias repositorioProvincias) {
        this.jpaRepository = jpaRepository;
        this.repositorioProvincias = repositorioProvincias;
    }

    public Ciudad guardar(Ciudad ciudad) {
        return jpaRepository.save(ciudad);
    }

    public Optional<Ciudad> buscarPorNombreYProvincia(String nombre, String nombreProvincia) {
        return jpaRepository.findByNombreAndProvinciaNombre(nombre, nombreProvincia);
    }

    // Antes vivía en GestorDirecciones: busca la Ciudad por nombre (dentro de su Provincia,
    // dentro de su Pais); si algún nivel no existe todavía, lo crea y lo persiste. Evita duplicar
    // el catálogo geográfico compartido en cada alta. Se movió acá (repositorio "punta" de la
    // cadena Pais/Provincia/Ciudad) para no mantener un gestor que solo coordinaba 3 repos.
    public Ciudad obtenerOCrearCiudad(String nombrePais, String nombreProvincia, String nombreCiudad) {
        String paisNombre = (nombrePais != null && !nombrePais.isBlank()) ? nombrePais : "Argentina";
        String provinciaNombre = (nombreProvincia != null && !nombreProvincia.isBlank()) ? nombreProvincia : "Buenos Aires";
        String ciudadNombre = (nombreCiudad != null && !nombreCiudad.isBlank()) ? nombreCiudad : "CABA";

        return buscarPorNombreYProvincia(ciudadNombre, provinciaNombre)
                .orElseGet(() -> {
                    Provincia provincia = repositorioProvincias.obtenerOCrearProvincia(provinciaNombre, paisNombre);
                    return guardar(new Ciudad(ciudadNombre, provincia));
                });
    }
}
