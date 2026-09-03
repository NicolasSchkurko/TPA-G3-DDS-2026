package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioCategoriasDeBienes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioSubcategoriasDeBienes;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestorNecesidades {

  private final RepositorioNecesidades repositorio;
  private final RepositorioCategoriasDeBienes repositorioCategorias;
  private final RepositorioSubcategoriasDeBienes repositorioSubcategorias;

  public GestorNecesidades(
      RepositorioNecesidades repositorio,
      RepositorioCategoriasDeBienes repositorioCategorias,
      RepositorioSubcategoriasDeBienes repositorioSubcategorias
  ) {
    this.repositorio = repositorio;
    this.repositorioCategorias = repositorioCategorias;
    this.repositorioSubcategorias = repositorioSubcategorias;
  }

  /**
   * Busca una SubcategoriaBien por nombre (dentro de su CategoriaBien); si no existe,
   * crea la CategoriaBien (si hace falta) y la SubcategoriaBien y las persiste.
   * Evita que cada Necesidad creada duplique entradas del catálogo de Bienes.
   */
  public SubcategoriaBien obtenerOCrearSubcategoria(String nombreCategoria, String nombreSubcategoria) {
    String catNombre = (nombreCategoria != null && !nombreCategoria.isBlank()) ? nombreCategoria : "General";
    String subNombre = (nombreSubcategoria != null && !nombreSubcategoria.isBlank()) ? nombreSubcategoria : "General";

    return repositorioSubcategorias.buscarPorNombreYCategoria(subNombre, catNombre)
        .orElseGet(() -> {
          CategoriaBien categoria = repositorioCategorias.buscarPorNombre(catNombre)
              .orElseGet(() -> repositorioCategorias.guardar(new CategoriaBien(catNombre)));
          return repositorioSubcategorias.guardar(new SubcategoriaBien(subNombre, categoria));
        });
  }

  public Necesidad modificarNecesidad(UUID idOriginal, Necesidad datosNuevos) {
    Necesidad existente = repositorio.buscarPorId(idOriginal).get();
    if (repositorio.buscarPorId(idOriginal).isEmpty()) {
      throw new IllegalArgumentException("No se encontró la entidad con ID: " + idOriginal);
    }

    existente.setCantidadObjetivo(datosNuevos.getCantidadObjetivo());
    existente.setDescripcion(datosNuevos.getDescripcion());
    existente.setSubcategoria(datosNuevos.getSubcategoria());

    if (datosNuevos instanceof NecesidadRecurrente) {
      ((NecesidadRecurrente) existente).setPlazoEnDias(((NecesidadRecurrente) datosNuevos).getPlazoEnDias());
    }

    try {
      repositorio.actualizar(idOriginal, existente);
      System.out.println("Necesidad actualizada con éxito.");
    } catch (IllegalArgumentException e) {
      System.err.println("Error al modificar necesidad: " + e.getMessage());
    }

    return existente;
  }
}

