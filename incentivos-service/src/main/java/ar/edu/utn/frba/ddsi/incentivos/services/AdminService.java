package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.controllers.request.CategoriaFiltroRequest;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.ConstanciaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.OperacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
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

@Service
public class AdminService {
    private final RepositorioCategorias repoCategorias;
    private final RepositorioMisiones repoMisiones;
    private final SecuenciaCategoria gestorSecuencia;
    private final DonacionClient donacionClient;
    private final SincronizacionPerfiles gestorSincronizacion;
    private final MisionFactory misionFactory;

    public AdminService(RepositorioCategorias repoCategorias,
                        RepositorioMisiones repoMisiones,
                        SecuenciaCategoria gestorSecuencia,
                        MisionFactory misionFactory,
                        DonacionClient donacionClient,
                        SincronizacionPerfiles gestorSincronizacion) {
        this.repoCategorias = repoCategorias;
        this.repoMisiones = repoMisiones;
        this.gestorSecuencia = gestorSecuencia;
        this.misionFactory = misionFactory;
        this.donacionClient = donacionClient;
        this.gestorSincronizacion = gestorSincronizacion;
    }

    private void verificarPermisos(UUID idAdmin) {
        if (!donacionClient.verificarAdmin(idAdmin)) {
            throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
        }
    }

    public Page<CategoriaDTO> obtenerCategorias(CategoriaFiltroRequest filtros, Pageable pageable) {
        return repoCategorias.obtenerTodas(
            filtros.nombre(),
            filtros.posicionSecuencia(),
            filtros.misionId(),
            pageable
        ).map(this::categoriaToDTO);
    }

    public CategoriaDTO obtenerCategoriaPorId(UUID id) {
        Categoria categoria = repoCategorias.obtenerPorId(id);
        return categoria != null ? categoriaToDTO(categoria) : null;
    }

    @Transactional
    public CategoriaDTO agregarCategoria(UUID idAdmin, CategoriaDTO dto) {
        verificarPermisos(idAdmin);
        List<Mision> misiones = repoMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );

        gestorSecuencia.desplazarParaCrear(repoCategorias,categoria.getPosicionSecuencia());
        Categoria categoriaCreada = repoCategorias.save(categoria);

        return categoriaToDTO(categoriaCreada);
    }

    @Transactional
    public CategoriaDTO actualizarCategoria(UUID idAdmin, UUID id, CategoriaDTO dto) {
        verificarPermisos(idAdmin);

        List<Mision> misiones = repoMisiones.conseguirMisiones(dto.getMisiones());
        Categoria categoriaModificada = new Categoria(
            dto.getNombre(), idAdmin,
            dto.getPosicionSecuencia(), misiones
        );

        Categoria actualizada = repoCategorias.findById(id).map(categoriaActual -> {
            Map<UUID, Integer> posicionesAnteriores = categoriaActual.getCategoriaMisiones().stream()
                                                                     .collect(java.util.stream.Collectors.toMap(
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
        }).orElse(null);

        return actualizada != null ? categoriaToDTO(actualizada) : null;
    }

    @Transactional
    public void eliminarCategoria(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        Categoria categoria = repoCategorias.obtenerPorId(id);
        if (categoria == null) {
            throw new EntityNotFoundException("No se encontró la categoría con ID: " + id);
        }

        Integer posicionLiberada = categoria.getPosicionSecuencia();

        repoCategorias.delete(categoria);
        gestorSecuencia.desplazarParaEliminar(repoCategorias,posicionLiberada);
    }

    public List<MisionDTO> obtenerMisiones() {
        return repoMisiones.findAll().stream()
                           .map(this::misionToDTO)
                           .toList();
    }

    public MisionDTO obtenerMisionPorId( UUID id) {
        Mision mision = repoMisiones.obtenerPorId(id);
        return mision != null ? misionToDTO(mision) : null;
    }

    @Transactional
    public MisionDTO crearMision(UUID idAdmin, MisionDTO dto) {
        verificarPermisos(idAdmin);

        AtributoImpacto atributoImpacto = misionFactory.crearAtributoImpacto(dto.getRegla().getAtributo());

        Mision mision = misionFactory.crearMision(
            idAdmin, dto.getNombreMision(), dto.getDescripcion(),  dto.getInsigniaObjetivo(),
            misionFactory.crearConstancia(
                dto.getRegla().getConstancia().getCantidad(),
                dto.getRegla().getConstancia().getUnidadTiempo()
            ),
            atributoImpacto,
            misionFactory.crearOperacion(
                dto.getRegla().getOperacion().getTipoOperacion(),
                dto.getRegla().getOperacion().getProgresoObjetivo(),
                dto.getRegla().getOperacion().getCantidad(),
                dto.getRegla().getOperacion().getValorEsperado()
            )
        );
        repoMisiones.save(mision);
        return misionToDTO(mision);
    }

    @Transactional
    public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
        verificarPermisos(idAdmin);

        AtributoImpacto atributoImpacto = misionFactory.crearAtributoImpacto(dto.getRegla().getAtributo());

        Mision mision = misionFactory.crearMision(
            idAdmin, dto.getNombreMision(), dto.getDescripcion(),  dto.getInsigniaObjetivo(),
            misionFactory.crearConstancia(
                dto.getRegla().getConstancia().getCantidad(),
                dto.getRegla().getConstancia().getUnidadTiempo()
            ),
            atributoImpacto,
            misionFactory.crearOperacion(
                dto.getRegla().getOperacion().getTipoOperacion(),
                dto.getRegla().getOperacion().getProgresoObjetivo(),
                dto.getRegla().getOperacion().getCantidad(),
                dto.getRegla().getOperacion().getValorEsperado()
            )
        );

        if (idMision != null) {
            mision.setIdMision(idMision);
        }

        Mision misionActual = repoMisiones.findById(mision.getIdMision()).orElse(null);

        if (misionActual != null) {
            Mision actualizada = repoMisiones.actualizarMision(misionActual, mision);
            gestorSincronizacion.reiniciarProgresoDeMision(actualizada.getIdMision());
            return misionToDTO(actualizada);
        }

        return null;
    }

    @Transactional
    public MisionDTO eliminarMision(UUID idAdmin, UUID idMision) {
        verificarPermisos(idAdmin);
        return misionToDTO(repoMisiones.eliminarMision(idMision));
    }

    private CategoriaDTO categoriaToDTO(Categoria categoria) {
        List<UUID> idMisiones = categoria.getCategoriaMisiones().stream()
                                         .map(cm -> cm.getMision().getIdMision())
                                         .toList();

        return new CategoriaDTO(
            categoria.getNombre(),
            categoria.getPosicionSecuencia(),
            idMisiones
        );
    }

    private MisionDTO misionToDTO(Mision mision) {
        if (mision == null) {
            return null;
        }

        ReglaConstancia reglaConstancia = mision.getReglaDeProgreso().getConstancia();
        ConstanciaDTO constancia = reglaConstancia == null
                                   ? null
                                   : new ConstanciaDTO(
            reglaConstancia.getCantidad(),
            reglaConstancia.getUnidadTiempo().toString()
        );

        return new MisionDTO(
            mision.getNombreMision(),
            mision.getDescripcion(),
            mision.getInsigniaObjetivo().getNombre(),
            constancia,
            mision.getReglaDeProgreso().getAtributo().name(),
            operacionToDTO(mision.getReglaDeProgreso().getOperacion())
        );
    }

    private OperacionDTO operacionToDTO(Operacion operacion) {
        if (operacion instanceof CantidadCoincidencias coincidencias) {
            return new OperacionDTO(
                "COINCIDENCIAS",
                coincidencias.getProgresoObjetivo(),
                String.valueOf(coincidencias.getValorEsperado()),
                null
            );
        }

        if (operacion instanceof ValoresDistintos distintos) {
            return new OperacionDTO(
                "VALORES_DISTINTOS",
                distintos.getProgresoObjetivo(),
                null,
                distintos.getCantValoresDistintos()
            );
        }

        if (operacion instanceof SuperaCantidad superaCantidad) {
            return new OperacionDTO(
                "SUPERA_CANTIDAD",
                superaCantidad.getProgresoObjetivo(),
                null,
                superaCantidad.getCantidadEsperada()
            );
        }

        throw new IllegalArgumentException(
            "Tipo de operación no soportado: " + operacion.getClass().getSimpleName()
        );
    }
}