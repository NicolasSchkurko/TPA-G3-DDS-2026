package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.controllers.request.CategoriaFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.CategoriaMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, edición, consulta y borrado de categorías.
 *
 * <p>Lo que este servicio garantiza, y que es lo que lo hace más que un CRUD, son las tres
 * invariantes de la secuencia: no puede haber dos categorías en la misma posición, no puede
 * haber huecos, y no se puede borrar una que todavía tiene donantes. Las tres se verifican
 * <em>antes</em> de escribir nada, así que un error no deja la secuencia a medias.
 */
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

    /**
     * Lista paginada, con filtro opcional por nombre, posición o misión incluida.
     *
     * <p>La búsqueda por nombre es parcial y sin distinguir mayúsculas; los filtros que no
     * vienen se ignoran en vez de filtrar por null.
     */
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> obtenerCategorias(CategoriaFiltroRequest filtros, Pageable pageable) {
        return repoCategorias.obtenerTodas(
                filtros.nombre(),
                filtros.posicionSecuencia(),
                filtros.misionId(),
                pageable
        ).map(CategoriaDTO::desdeEntidad);
    }

    /** Una categoría por id. */
    @Transactional(readOnly = true)
    public CategoriaDTO obtenerCategoriaPorId(UUID id) {
        Categoria categoria = repoCategorias.obtenerPorId(id);
        if (categoria == null) {
            throw new InexistenteException();
        }
        return CategoriaDTO.desdeEntidad(categoria);
    }

    /**
     * Crea una categoría y abre un hueco en la secuencia en su posición.
     *
     * <p>Si no se pasa posición, la nueva queda al final del programa. Si se pasa y ya está
     * ocupada, se rechaza con {@link ConflictoException} (409) antes de desplazar nada.
     */
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

    /**
     * Edita una categoría: nombre, posición en el programa y secuencia de misiones.
     *
     * <p>Si cambian las misiones o sus posiciones, {@link SincronizacionPerfiles} le
     * reacomoda la misión a los donantes que estaban en la posición anterior (punto 19).
     */
    @Transactional
    public CategoriaDTO actualizarCategoria(UUID idAdmin, UUID id, CategoriaDTO dto) {
        validadorAdmin.verificarPermisos(idAdmin);

        Categoria categoriaActual = repoCategorias.findById(id)
                .orElseThrow(() -> new InexistenteException(
                        "No se encontró la categoría con ID: " + id));

        List<Mision> misiones = repoMisiones.conseguirMisiones(dto.getMisiones());
        Categoria categoriaModificada = new Categoria(
                dto.getNombre(),
                idAdmin,
                dto.getPosicionSecuencia(),
                misiones
        );

        // Las posiciones que tenía cada misión ANTES del cambio: son las que usa la
        // sincronización para saber a quién hay que correrle la secuencia.
        Map<UUID, Integer> posicionesAnteriores = categoriaActual.getCategoriaMisiones().stream()
                .collect(Collectors.toMap(
                        cm -> cm.getMision().getIdMision(),
                        CategoriaMision::getPosicion,
                        (primera, segunda) -> primera
                ));

        Integer destino = categoriaModificada.getPosicionSecuencia();
        if (destino != null) {
            // La posición tiene que estar libre, salvo que sea la misma que ya tenía esta
            // categoría (el caso normal de editar el nombre).
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
            categoriaActual.moverAPosicion(destino);
        }

        categoriaActual.actualizarCon(categoriaModificada);
        gestorSincronizacion.actualizarMisionesPorCambioDeCategoria(
                categoriaActual,
                posicionesAnteriores
        );

        return CategoriaDTO.desdeEntidad(repoCategorias.save(categoriaActual));
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
