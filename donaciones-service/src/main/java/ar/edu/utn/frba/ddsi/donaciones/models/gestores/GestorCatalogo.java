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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve las entidades de catálogo compartidas: país/provincia/ciudad y
 * categoría/subcategoría. Es coordinación entre repositorios, no persistencia de una entidad.
 *
 * Para subcategorías hay dos operaciones y la diferencia importa: obtenerOCrearSubcategoria crea
 * lo que falte (alta de catálogo, o sea necesidades) y obtenerSubcategoriaExistente falla si no
 * está (alta de bienes de una donación). Ver el comentario de cada una.
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

    /**
     * Resuelve la subcategoría y, si no existe, la crea. Es la vía del ALTA DE CATÁLOGO: la usan
     * las necesidades, que son las que definen qué se puede pedir. Una entidad que necesita algo
     * que nadie donó todavía tiene que poder nombrarlo.
     *
     * No la uses para resolver los bienes de una donación: ver obtenerSubcategoriaExistente.
     */
    @Transactional
    public SubcategoriaBien obtenerOCrearSubcategoria(String nombreCategoria, String nombreSubcategoria) {
        String catNombre = normalizar(nombreCategoria, "General");
        String subNombre = normalizar(nombreSubcategoria, "General");

        // La búsqueda laxa va primero para no duplicar por mayúsculas ("ENLATADOS" vs "Enlatados"):
        // si ya hay una equivalente se reusa, y recién si no hay nada se crea con el nombre tal
        // como lo escribieron.
        return subcategorias.buscarPorNombreYCategoriaIgnorandoCase(subNombre, catNombre)
                .orElseGet(() -> subcategorias.guardar(
                        new SubcategoriaBien(subNombre, categorias.obtenerOCrearCategoria(catNombre))));
    }

    /**
     * Resuelve la subcategoría contra el catálogo existente y falla si no está. Es la vía de las
     * DONACIONES.
     *
     * Por qué no alcanza con obtenerOCrearSubcategoria: el asignador empareja donaciones con
     * necesidades por nombre de subcategoría (Necesidad.esCompatibleCon usa equals()). Si el alta
     * de una donación puede inventar subcategorías, un simple error de tipeo —"Frazada" por
     * "Frazadas"— crea una subcategoría nueva, el alta responde OK y la donación queda para
     * siempre en depósito sin match posible y sin un solo mensaje de error. El síntoma aparece
     * recién al correr el asignador, lejos de la causa.
     *
     * Así que acá se resuelve o se explota, y el mensaje lista las opciones válidas para que el
     * error sea accionable.
     */
    @Transactional(readOnly = true)
    public SubcategoriaBien obtenerSubcategoriaExistente(String nombreCategoria, String nombreSubcategoria) {
        String subNombre = normalizar(nombreSubcategoria, null);
        if (subNombre == null) {
            throw new IllegalArgumentException("Falta la subcategoría del bien. " + opcionesDelCatalogo());
        }
        String catNombre = normalizar(nombreCategoria, null);

        if (catNombre != null) {
            Optional<SubcategoriaBien> conCategoria =
                    subcategorias.buscarPorNombreYCategoriaIgnorandoCase(subNombre, catNombre);
            if (conCategoria.isPresent()) {
                return conCategoria.get();
            }
        }

        // Sin categoría (o con una que no coincide) todavía se puede resolver por el nombre de la
        // subcategoría, que es lo único que mira el asignador. Pero sólo si no es ambiguo: si el
        // mismo nombre cuelga de dos categorías, elegir una por nosotros sería adivinar.
        List<SubcategoriaBien> porNombre = subcategorias.buscarPorNombreIgnorandoCase(subNombre);
        if (porNombre.size() == 1) {
            return porNombre.get(0);
        }
        if (porNombre.size() > 1) {
            String categoriasPosibles = porNombre.stream()
                    .map(s -> s.getCategoria() != null ? s.getCategoria().getNombre() : "(sin categoría)")
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("La subcategoría \"" + subNombre
                    + "\" existe en más de una categoría (" + categoriasPosibles
                    + "): indicá cuál corresponde.");
        }

        throw new IllegalArgumentException("La subcategoría \"" + subNombre + "\""
                + (catNombre != null ? " (categoría \"" + catNombre + "\")" : "")
                + " no existe en el catálogo. " + opcionesDelCatalogo()
                + " Si de verdad hace falta una nueva, la crea la entidad al cargar la necesidad:"
                + " una donación de algo que ninguna entidad pidió no tiene a quién ir.");
    }

    /** Pares categoría/subcategoría cargados, para que el error diga qué sí se puede elegir. */
    private String opcionesDelCatalogo() {
        String opciones = subcategorias.findAll().stream()
                .map(s -> (s.getCategoria() != null ? s.getCategoria().getNombre() + " / " : "")
                        + s.getNombre())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
        return opciones.isBlank()
                ? "El catálogo de subcategorías está vacío todavía: no hay ninguna necesidad cargada."
                : "Opciones válidas: " + opciones + ".";
    }

    /** Recorta los espacios de los extremos y cae al default si no quedó nada. */
    private static String normalizar(String valor, String porDefecto) {
        if (valor == null) {
            return porDefecto;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? porDefecto : limpio;
    }
}
