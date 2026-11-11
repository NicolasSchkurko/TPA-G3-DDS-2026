package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeCategoriasDeBienes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDePaises;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeProvincias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeSubcategoriasDeBienes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve (o crea) las entidades de catálogo compartidas: país/provincia/ciudad y
 * categoría/subcategoría. Es coordinación entre repositorios, no persistencia de una entidad.
 */
@Service
public class GestorCatalogo {

    private final RepositorioDePaises paises;
    private final RepositorioDeProvincias provincias;
    private final RepositorioDeCiudades ciudades;
    private final RepositorioDeCategoriasDeBienes categorias;
    private final RepositorioDeSubcategoriasDeBienes subcategorias;

    public GestorCatalogo(RepositorioDePaises paises,
                          RepositorioDeProvincias provincias,
                          RepositorioDeCiudades ciudades,
                          RepositorioDeCategoriasDeBienes categorias,
                          RepositorioDeSubcategoriasDeBienes subcategorias) {
        this.paises = paises;
        this.provincias = provincias;
        this.ciudades = ciudades;
        this.categorias = categorias;
        this.subcategorias = subcategorias;
    }

    @Transactional
    public Ciudad obtenerOCrearCiudad(String nombrePais, String nombreProvincia, String nombreCiudad) {
        String paisNombre = (nombrePais != null && !nombrePais.isBlank()) ? nombrePais : "Argentina";
        String provinciaNombre = (nombreProvincia != null && !nombreProvincia.isBlank())
                ? nombreProvincia : "Buenos Aires";
        String ciudadNombre = (nombreCiudad != null && !nombreCiudad.isBlank()) ? nombreCiudad : "CABA";

        return ciudades.buscarPorNombreYProvincia(ciudadNombre, provinciaNombre)
                .orElseGet(() -> ciudades.guardar(
                        new Ciudad(ciudadNombre, obtenerOCrearProvincia(provinciaNombre, paisNombre))));
    }

    private Provincia obtenerOCrearProvincia(String nombreProvincia, String nombrePais) {
        return provincias.buscarPorNombreYPais(nombreProvincia, nombrePais)
                .orElseGet(() -> provincias.guardar(
                        new Provincia(nombreProvincia, obtenerOCrearPais(nombrePais))));
    }

    private Pais obtenerOCrearPais(String nombrePais) {
        return paises.obtenerOCrearPais(nombrePais);
    }

    @Transactional
    public SubcategoriaBien obtenerOCrearSubcategoria(String nombreCategoria, String nombreSubcategoria) {
        String catNombre = (nombreCategoria != null && !nombreCategoria.isBlank())
                ? nombreCategoria : "General";
        String subNombre = (nombreSubcategoria != null && !nombreSubcategoria.isBlank())
                ? nombreSubcategoria : "General";

        return subcategorias.buscarPorNombreYCategoria(subNombre, catNombre)
                .orElseGet(() -> subcategorias.guardar(
                        new SubcategoriaBien(subNombre, categorias.obtenerOCrearCategoria(catNombre))));
    }
}
