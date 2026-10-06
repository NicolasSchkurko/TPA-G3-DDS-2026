package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.controllers.request.MisionFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.ConstanciaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.OperacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.ReglaDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, edición, consulta y borrado de misiones.
 *
 * <p>Dos cosas que este servicio cuida y no son CRUD:
 *
 * <ul>
 *   <li>Editar una misión solo borra el avance de los donantes si cambió <em>el criterio
 *       de completado</em>, no si se retocó el texto. Antes se borraba siempre, y con eso
 *       corregir una descripción le costaba el progreso a todos los que estaban por
 *       completarla (punto 15).
 *   <li>Borrar una misión exige que nadie la haya completado ni la esté haciendo, porque
 *       las insignias ya otorgadas la referencian (punto 18).
 * </ul>
 */
@Service
public class MisionService {
    private final RepositorioMisiones repoMisiones;
    private final RepositorioPerfiles repoPerfiles;
    private final RepositorioCategorias repoCategorias;
    private final MisionFactory misionFactory;
    private final SincronizacionPerfiles gestorSincronizacion;
    private final ValidadorAdmin validadorAdmin;

    public MisionService(RepositorioMisiones repoMisiones,
                       RepositorioPerfiles repoPerfiles,
                       RepositorioCategorias repoCategorias,
                       MisionFactory misionFactory,
                       SincronizacionPerfiles gestorSincronizacion,
                       ValidadorAdmin validadorAdmin) {
        this.repoMisiones = repoMisiones;
        this.repoPerfiles = repoPerfiles;
        this.repoCategorias = repoCategorias;
        this.misionFactory = misionFactory;
        this.gestorSincronizacion = gestorSincronizacion;
        this.validadorAdmin = validadorAdmin;
    }

    /** Lista paginada, con filtro opcional por nombre, insignia objetivo o atributo. */
    @Transactional(readOnly = true)
    public Page<MisionDTO> obtenerMisiones(MisionFiltroRequest filtros, Pageable pageable) {
        return repoMisiones.obtenerTodas(
                filtros.nombreMision(),
                filtros.insigniaObjetivo(),
                filtros.atributo(),
                pageable
        ).map(MisionDTO::desdeEntidad);
    }

    /** Una misión por id. */
    @Transactional(readOnly = true)
    public MisionDTO obtenerMisionPorId(UUID id) {
        Mision mision = repoMisiones.obtenerPorId(id);
        if (mision == null) {
            throw new InexistenteException();
        }
        return MisionDTO.desdeEntidad(mision);
    }

    /**
     * Crea una misión a partir del DTO del admin.
     *
     * <p>La traducción del JSON a la jerarquía de regla y operación es trabajo de
     * {@link MisionFactory}, incluida la validación de los textos libres.
     */
    @Transactional
    public MisionDTO crearMision(UUID idAdmin, MisionDTO dto) {
        validadorAdmin.verificarPermisos(idAdmin);
        Mision mision = construirMision(idAdmin, dto);
        repoMisiones.save(mision);
        return MisionDTO.desdeEntidad(mision);
    }

    /**
     * Edita una misión. Si cambia lo que el donante tiene que cumplir, le
     * reinicia el avance a todos los que estaban en ella (punto 15).
     */
    @Transactional
    public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
        validadorAdmin.verificarPermisos(idAdmin);

        Mision misionActual = repoMisiones.findById(idMision)
                .orElseThrow(() -> new InexistenteException(
                        "No se encontró la misión con ID: " + idMision));

        Mision misionModificada = construirMision(idAdmin, dto);

        // Solo se borra el avance si cambió lo que el donante tiene que cumplir.
        // Antes se hacía siempre, y con eso retocar la descripción de una
        // misión le costaba el progreso a todos los que estaban por completarla, sin aviso
        // (punto 15).
        boolean cambioElCriterio = misionActual.actualizar(misionModificada);
        Mision actualizada = repoMisiones.save(misionActual);

        if (cambioElCriterio) {
            gestorSincronizacion.reiniciarProgresoDeMision(actualizada.getIdMision());
        }

        return MisionDTO.desdeEntidad(actualizada);
    }

    /**
     * Borra una misión, salvo que alguien la haya completado o la esté haciendo.
     *
     * <p>Antes no había ninguna guarda: {@code Mision.insigniaObjetivo} tiene cascada, así
     * que al borrar la misión se iba también su insignia, pero
     * {@code InsigniaObtenida.insignia} es un {@code ManyToOne} sin cascada y reventaba por
     * FK. O sea que no se podía borrar una misión que alguien ya había completado y el
     * error era un 500 sin explicación. Y si la misión no existía, el controller respondía
     * 204 igual (punto 18).
     */
    @Transactional
    public void eliminarMision(UUID idAdmin, UUID idMision) {
        validadorAdmin.verificarPermisos(idAdmin);

        Mision mision = repoMisiones.findById(idMision)
                .orElseThrow(() -> new InexistenteException(
                        "No se encontró la misión con ID: " + idMision));

        long haciendo = repoPerfiles.countByProgresoMisionActualMision(mision);
        if (haciendo > 0) {
            throw new ConflictoException(
                    "La misión '" + mision.getNombreMision() + "' no se puede borrar: "
                            + haciendo + " donante(s) están haciendo esa misión.");
        }

        long yaLaCompletaron = repoPerfiles.countByInsigniasObtenidasInsignia(mision.getInsigniaObjetivo());
        if (yaLaCompletaron > 0) {
            throw new ConflictoException("La misión '" + mision.getNombreMision() + "' no se puede borrar: "
                            + yaLaCompletaron + " donante(s) ya obtuvieron su insignia.");
        }

        // CategoriaMision es el lado propietario y no tiene cascada, asi que la referencia
        // tiene que soltarse antes del delete. Sin esto, borrar cualquier mision que pertenezca
        // a una categoria revienta por FK y el endpoint documentado como 204 nunca podia
        // responder 204.
        List<Categoria> categoriasConLaMision = repoCategorias.findAllByCategoriaMisionesMision(mision);
        categoriasConLaMision.forEach(categoria -> {
            categoria.eliminarMision(mision);
            repoCategorias.save(categoria);
        });

        repoMisiones.delete(mision);
    }

    /**
     * Traduce el DTO a la entidad. Las anotaciones de Bean Validation ya cubren esto
     * cuando el pedido viene por HTTP, pero el service se puede llamar desde código y
     * un null acá terminaba en NullPointerException (500) en vez de un 400.
     */
    private Mision construirMision(UUID idAdmin, MisionDTO dto) {
        ReglaDTO regla = dto.getRegla();
        if (regla == null) {
            throw new DatosInvalidosException("La misión requiere una regla de progreso");
        }

        ConstanciaDTO constancia = regla.getConstancia();
        OperacionDTO operacion = regla.getOperacion();
        if (operacion == null) {
            throw new DatosInvalidosException("La regla de la misión requiere una operación");
        }

        return misionFactory.crearMision(
                idAdmin,
                dto.getNombreMision(),
                dto.getDescripcion(),
                dto.getInsigniaObjetivo(),
                constancia != null ? constancia.getCantidad() : null,
                constancia != null ? constancia.getUnidadTiempo() : null,
                regla.getAtributo(),
                operacion.getTipoOperacion(),
                operacion.getProgresoObjetivo(),
                operacion.getCantidad(),
                operacion.getValorEsperado()
        );
    }
}
