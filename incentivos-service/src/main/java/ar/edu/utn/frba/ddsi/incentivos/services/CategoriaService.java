package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.controllers.request.CategoriaFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoriaService {
  private final RepositorioCategorias repoCategorias;
  private final RepositorioMisiones repoMisiones;
  private final SecuenciaCategoria gestorSecuencia;
  private final SincronizacionPerfiles gestorSincronizacion;
  private final ValidadorAdmin validadorAdmin;

  public CategoriaService(RepositorioCategorias repoCategorias,
                          RepositorioMisiones repoMisiones,
                          SecuenciaCategoria gestorSecuencia,
                          SincronizacionPerfiles gestorSincronizacion,
                          ValidadorAdmin validadorAdmin) {
    this.repoCategorias = repoCategorias;
    this.repoMisiones = repoMisiones;
    this.gestorSecuencia = gestorSecuencia;
    this.gestorSincronizacion = gestorSincronizacion;
    this.validadorAdmin = validadorAdmin;
  }

  @Transactional(readOnly = true)
    public Page<CategoriaDTO> obtenerCategorias(CategoriaFiltroRequest filtros, Pageable pageable) {
    return repoCategorias.obtenerTodas(
        filtros.nombre(),
        filtros.posicionSecuencia(),
        filtros.misionId(),
        pageable
    ).map(CategoriaDTO::desdeEntidad);
  }

  @Transactional(readOnly = true)
    public CategoriaDTO obtenerCategoriaPorId(UUID id) {
    Categoria categoria = repoCategorias.obtenerPorId(id);
    return CategoriaDTO.desdeEntidad(categoria);
  }

  @Transactional
  public CategoriaDTO agregarCategoria(UUID idAdmin, CategoriaDTO dto) {
    validadorAdmin.verificarPermisos(idAdmin);
    List<Mision> misiones = repoMisiones.conseguirMisiones(dto.getMisiones());

    Categoria categoria = new Categoria(
        dto.getNombre(),
        idAdmin,
        dto.getPosicionSecuencia(),
        misiones
    );

    gestorSecuencia.desplazarParaCrear(repoCategorias, categoria.getPosicionSecuencia());
    Categoria categoriaCreada = repoCategorias.save(categoria);

    return CategoriaDTO.desdeEntidad(categoriaCreada);
  }

  @Transactional
  public CategoriaDTO actualizarCategoria(UUID idAdmin, UUID id, CategoriaDTO dto) {
    validadorAdmin.verificarPermisos(idAdmin);

    return repoCategorias.findById(id).map(categoriaActual -> {
                           List<Mision> misiones = repoMisiones.conseguirMisiones(dto.getMisiones());
                           Categoria categoriaModificada = new Categoria(
                               dto.getNombre(),
                               idAdmin,
                               dto.getPosicionSecuencia(),
                               misiones
                           );

                           Map<UUID, Integer> posicionesAnteriores = categoriaActual.getCategoriaMisiones().stream()
                                                                                    .collect(Collectors.toMap(
                                                                                        cm -> cm.getMision().getIdMision(),
                                                                                        cm -> cm.getPosicion()
                                                                                    ));

                           if (categoriaModificada.getPosicionSecuencia() != null) {
                             gestorSecuencia.desplazarParaActualizar(
                                 repoCategorias,
                                 categoriaActual.getPosicionSecuencia(),
                                 categoriaModificada.getPosicionSecuencia(),
                                 repoCategorias.count()
                             );
                             categoriaActual.setPosicionSecuencia(categoriaModificada.getPosicionSecuencia());
                           }

                           categoriaActual.copiar(categoriaModificada);
                           gestorSincronizacion.actualizarMisionesPorCambioDeCategoria(
                               categoriaActual,
                               posicionesAnteriores
                           );
                           return repoCategorias.save(categoriaActual);
                         })
                         .map(CategoriaDTO::desdeEntidad)
                         .orElse(null);
  }

  @Transactional
  public void eliminarCategoria(UUID idAdmin, UUID id) {
    validadorAdmin.verificarPermisos(idAdmin);

    Categoria categoria = repoCategorias.obtenerPorId(id);
    if (categoria == null) {
      throw new EntityNotFoundException("No se encontró la categoría con ID: " + id);
    }

    Integer posicionLiberada = categoria.getPosicionSecuencia();

    repoCategorias.delete(categoria);
    gestorSecuencia.desplazarParaEliminar(repoCategorias, posicionLiberada);
  }
}
