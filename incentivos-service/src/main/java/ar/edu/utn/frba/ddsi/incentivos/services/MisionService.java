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
 * Alta, edición, consulta y borrado de misiones. Editar solo reinicia el avance si cambió el
 * criterio de completado, y borrar exige que nadie la haya completado ni la esté haciendo.
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
     * Crea una misión a partir del DTO del admin. La traducción a regla y operación la hace
     * {@link MisionFactory}.
     */
    @Transactional
    public MisionDTO crearMision(UUID idAdmin, MisionDTO dto) {
        validadorAdmin.verificarPermisos(idAdmin);
        Mision mision = construirMision(idAdmin, dto);
        repoMisiones.save(mision);
        return MisionDTO.desdeEntidad(mision);
    }

    /**
     * Edita una misión. Si cambia lo que el donante tiene que cumplir, reinicia el avance a
     * todos los que estaban en ella.
     */
    @Transactional
    public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
        validadorAdmin.verificarPermisos(idAdmin);

        Mision misionActual = repoMisiones.findById(idMision)
                .orElseThrow(() -> new InexistenteException(
                        "No se encontró la misión con ID: " + idMision));

        Mision misionModificada = construirMision(idAdmin, dto);

        // Solo se borra el avance si cambió lo que el donante tiene que cumplir.
        boolean cambioElCriterio = misionActual.actualizar(misionModificada);
        Mision actualizada = repoMisiones.save(misionActual);

        if (cambioElCriterio) {
            gestorSincronizacion.reiniciarProgresoDeMision(actualizada.getIdMision());
        }

        return MisionDTO.desdeEntidad(actualizada);
    }

    /**
     * Borra una misión, salvo que alguien la haya completado o la esté haciendo: las
     * insignias ya otorgadas la referencian.
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

        // CategoriaMision es el lado propietario y no tiene cascada: hay que soltar la
        // referencia antes del delete.
        List<Categoria> categoriasConLaMision = repoCategorias.findAllByCategoriaMisionesMision(mision);
        categoriasConLaMision.forEach(categoria -> {
            categoria.eliminarMision(mision);
            repoCategorias.save(categoria);
        });

        repoMisiones.delete(mision);
    }

    /**
     * Traduce el DTO a la entidad. Valida los nulls que Bean Validation no cubre cuando se
     * llama desde código.
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
                // Los dos siguientes son de la insignia, no de la misión.
                dto.getInsigniaDescripcion(),
                dto.getInsigniaUrlImagen(),
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
