package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.controllers.request.CategoriaFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
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
  private final RepositorioPerfiles repoPerfiles;
  private final SecuenciaCategoria gestorSecuencia;
  private final SincronizacionPerfiles gestorSincronizacion;
  private final ValidadorAdmin validadorAdmin;

  public CategoriaService(RepositorioCategorias repoCategorias,
                          RepositorioMisiones repoMisiones,
                          RepositorioPerfiles repoPerfiles,
                          SecuenciaCategoria gestorSecuencia,
                          SincronizacionPerfiles gestorSincronizacion,
                          ValidadorAdmin validadorAdmin) {
    this.repoCategorias = repoCategorias;
    this.repoMisiones = repoMisiones;
    this.repoPerfiles = repoPerfiles;
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
    if (categoria == null) {
      throw new InexistenteException();
    }
    return CategoriaDTO.desdeEntidad(categoria);
  }

  @Transactional
  public CategoriaDTO agregarCategoria(UUID idAdmin, CategoriaDTO dto) {
    validadorAdmin.verificarPermisos(idAdmin);
    List<Mision> misiones = repoMisiones.conseguirMisiones(dto.getMisiones());

    Integer posicion = dto.getPosicionSecuencia();
    if (posicion != null && repoCategorias.existsByPosicionSecuencia(posicion)) {
      throw new ConflictoException("Ya hay una categoría en la posición " + posicion + ".");
    }

    Categoria categoria = new Categoria(
        dto.getNombre(),
        idAdmin,
        posicion,
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
                                                                                        cm -> cm.getPosicion(),
                                                                                        (primera, segunda) -> primera
                                                                                    ));

                           if (categoriaModificada.getPosicionSecuencia() != null) {
                             Integer destino = categoriaModificada.getPosicionSecuencia();

                             // La posición tiene que estar libre, salvo que sea la misma que
                             // ya tenía esta categoría (el caso normal de editar el nombre).
                             if (!destino.equals(categoriaActual.getPosicionSecuencia())
                                 && repoCategorias.existsByPosicionSecuencia(destino)) {
                               throw new ConflictoException(
                                       "Ya hay una categoría en la posición " + destino + ".");
                             }

                             gestorSecuencia.desplazarParaActualizar(
                                 repoCategorias,
                                 categoriaActual.getPosicionSecuencia(),
                                 destino,
                                 gestorSecuencia.posicionMaxima(repoCategorias)
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
                         .orElseThrow(InexistenteException::new);
  }

  /**
   * Borra una categoría, salvo que todavía tenga donantes asignados.
   *
   * <p>Sin la guarda, {@code Perfil.categoriaActual} es un {@code ManyToOne} y el borrado
   * reventaba por violación de FK: un 500 sin explicación. Además el borrado se frenaba
   * <em>después</em> de haber actualizado las posiciones de la secuencia, así que el
   * error dejaba la secuencia movida sin haber borrado nada. Ahora se verifica antes de
   * tocar nada (punto 18).
   */
  @Transactional
  public void eliminarCategoria(UUID idAdmin, UUID id) {
    validadorAdmin.verificarPermisos(idAdmin);

    Categoria categoria = repoCategorias.obtenerPorId(id);
    if (categoria == null) {
      // Antes salía EntityNotFoundException mientras el resto del servicio usa
      // InexistenteException. Los dos terminaban en 404, pero el mensaje era distinto
      // según por dónde se entrara.
      throw new InexistenteException("No se encontró la categoría con ID: " + id);
    }

    long donantes = repoPerfiles.countByCategoriaActual(categoria);
    if (donantes > 0) {
      throw new ConflictoException("La categoría '" + categoria.getNombre()
              + "' no se puede borrar: tiene " + donantes + " donante(s) asignados.");
    }

    repoCategorias.delete(categoria);
    gestorSecuencia.desplazarParaEliminar(repoCategorias, categoria.getPosicionSecuencia());
  }
}
