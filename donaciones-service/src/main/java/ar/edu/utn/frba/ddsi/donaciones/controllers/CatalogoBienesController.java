package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeCategoriasDeBienes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Catálogo de bienes", description = "Categorías y subcategorías compartidas por donaciones y necesidades")
public class CatalogoBienesController {

    private final RepositorioDeCategoriasDeBienes categorias;

    public CatalogoBienesController(RepositorioDeCategoriasDeBienes categorias) {
        this.categorias = categorias;
    }

    @Operation(summary = "Listar categorías con sus subcategorías")
    @Transactional(readOnly = true)
    @GetMapping
    public Map<String, List<String>> listar() {
        Map<String, List<String>> catalogo = new LinkedHashMap<>();
        categorias.findAll().stream()
                .sorted(Comparator.comparing(CategoriaBien::getNombre, String.CASE_INSENSITIVE_ORDER))
                .forEach(categoria -> catalogo.put(
                        categoria.getNombre(),
                        categoria.getSubcategorias() == null ? List.of()
                                : categoria.getSubcategorias().stream()
                                        .map(SubcategoriaBien::getNombre)
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .toList()));
        return catalogo;
    }
}
