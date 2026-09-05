package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioPaises;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioProvincias;
import org.springframework.stereotype.Service;

/**
 * Gestor para la cadena geográfica (Pais/Provincia/Ciudad), tratada como catálogo compartido:
 * no queremos que cada Direccion nueva duplique filas de Pais/Provincia/Ciudad ya existentes.
 */
@Service
public class GestorDirecciones {

  private final RepositorioPaises repositorioPaises;
  private final RepositorioProvincias repositorioProvincias;
  private final RepositorioCiudades repositorioCiudades;

  public GestorDirecciones(
      RepositorioPaises repositorioPaises,
      RepositorioProvincias repositorioProvincias,
      RepositorioCiudades repositorioCiudades
  ) {
    this.repositorioPaises = repositorioPaises;
    this.repositorioProvincias = repositorioProvincias;
    this.repositorioCiudades = repositorioCiudades;
  }

  /**
   * Busca la Ciudad por nombre (dentro de su Provincia, dentro de su Pais); si algún nivel
   * no existe todavía, lo crea y lo persiste. Evita duplicar el catálogo geográfico en cada alta.
   */
  public Ciudad obtenerOCrearCiudad(String nombrePais, String nombreProvincia, String nombreCiudad) {
    String paisNombre = (nombrePais != null && !nombrePais.isBlank()) ? nombrePais : "Argentina";
    String provinciaNombre = (nombreProvincia != null && !nombreProvincia.isBlank()) ? nombreProvincia : "Buenos Aires";
    String ciudadNombre = (nombreCiudad != null && !nombreCiudad.isBlank()) ? nombreCiudad : "CABA";

    return repositorioCiudades.buscarPorNombreYProvincia(ciudadNombre, provinciaNombre)
        .orElseGet(() -> {
          Provincia provincia = repositorioProvincias.buscarPorNombreYPais(provinciaNombre, paisNombre)
              .orElseGet(() -> {
                Pais pais = repositorioPaises.buscarPorNombre(paisNombre)
                    .orElseGet(() -> repositorioPaises.guardar(new Pais(paisNombre)));
                return repositorioProvincias.guardar(new Provincia(provinciaNombre, pais));
              });
          return repositorioCiudades.guardar(new Ciudad(ciudadNombre, provincia));
        });
  }
}
