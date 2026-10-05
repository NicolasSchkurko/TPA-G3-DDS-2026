package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.CategoriaMision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorSecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorSincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {
    private final RepositorioCategorias repoCategorias;
    private final RepositorioMisiones repoMisiones;
    private final GestorSecuenciaCategoria gestorSecuencia;
    private final DonacionClient donacionClient;
    private final GestorSincronizacionPerfiles gestorSincronizacion;
    private final RepositorioPerfiles repoPerfiles;
    private final MisionFactory misionFactory;

    public AdminService(RepositorioCategorias repoCategorias,
                        RepositorioMisiones repoMisiones,
                        GestorSecuenciaCategoria gestorSecuencia,
                        MisionFactory misionFactory,
                        DonacionClient donacionClient,
                        GestorSincronizacionPerfiles gestorSincronizacion,
                        RepositorioPerfiles repoPerfiles) {
        this.repoCategorias = repoCategorias;
        this.repoMisiones = repoMisiones;
        this.gestorSecuencia = gestorSecuencia;
        this.misionFactory = misionFactory;
        this.donacionClient = donacionClient;
        this.gestorSincronizacion = gestorSincronizacion;
        this.repoPerfiles = repoPerfiles;
    }

    private void verificarPermisos(UUID idAdmin) {
        if (!donacionClient.verificarAdmin(idAdmin)) {
            throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
        }
    }

    public List<CategoriaDTO> obtenerCategorias(UUID idAdmin) {
        verificarPermisos(idAdmin);
        return repoCategorias.obtenerTodas().stream()
                             .map(this::categoriaToDTO)
                             .toList();
    }

    public CategoriaDTO obtenerCategoriaPorId(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);
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

        gestorSecuencia.desplazarParaCrear(categoria.getPosicionSecuencia());
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
    public List<CategoriaDTO> eliminarCategoria(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        Categoria categoria = repoCategorias.findById(id).orElse(null);
        if (categoria == null) {
            return List.of();
        }

        Integer posicionLiberada = categoria.getPosicionSecuencia();
        Categoria categoriaSiguiente = posicionLiberada == null
            ? null
            : repoCategorias.findFirstByPosicionSecuenciaGreaterThanOrderByPosicionSecuenciaAsc(posicionLiberada)
                            .orElse(null);
        List<Perfil> perfiles = repoPerfiles.findAllByCategoriaActual(categoria);

        for (Perfil perfil : perfiles) {
            Mision misionAnterior = perfil.getProgresoMisionActual() != null
                ? perfil.getProgresoMisionActual().getMision()
                : null;
            MedioContacto contacto = categoriaSiguiente != null
                ? donacionClient.obtenerContactoPersona(perfil.getIdUsuario())
                : null;
            perfil.cambiarCategoria(categoriaSiguiente, categoria, misionAnterior, contacto);
        }

        repoPerfiles.saveAllAndFlush(perfiles);
        repoCategorias.delete(categoria);
        gestorSecuencia.desplazarParaEliminar(posicionLiberada);
        return repoCategorias.obtenerTodas().stream()
                             .map(this::categoriaToDTO)
                             .toList();
    }

    public List<MisionDTO> obtenerMisiones(UUID idAdmin) {
        verificarPermisos(idAdmin);
        return repoMisiones.findAll().stream()
                           .map(this::misionToDTO)
                           .toList();
    }

    public MisionDTO obtenerMisionPorId(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);
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
        Mision mision = repoMisiones.findById(idMision).orElse(null);
        if (mision == null) {
            return null;
        }

        MisionDTO misionEliminada = misionToDTO(mision);
        List<Perfil> perfiles = repoPerfiles.findAllByMisionActual(idMision);
        for (Perfil perfil : perfiles) {
            Mision misionSiguiente = obtenerMisionSiguiente(perfil.getCategoriaActual(), idMision);
            MedioContacto contacto = misionSiguiente != null
                ? donacionClient.obtenerContactoPersona(perfil.getIdUsuario())
                : null;
            perfil.cambiarMision(misionSiguiente, mision, contacto);
        }
        repoPerfiles.saveAllAndFlush(perfiles);

        List<Categoria> categoriasConMision = repoCategorias.findAllByMisionId(idMision);
        categoriasConMision.forEach(categoria -> categoria.eliminarMision(mision));
        repoCategorias.saveAllAndFlush(categoriasConMision);

        repoMisiones.eliminarMision(idMision);
        return misionEliminada;
    }

    private Mision obtenerMisionSiguiente(Categoria categoria, UUID idMisionEliminada) {
        if (categoria == null) {
            return null;
        }

        Integer posicionEliminada = categoria.getCategoriaMisiones().stream()
            .filter(cm -> cm.getMision().getIdMision().equals(idMisionEliminada))
            .map(CategoriaMision::getPosicion)
            .findFirst()
            .orElse(null);
        if (posicionEliminada == null) {
            return null;
        }

        return categoria.getCategoriaMisiones().stream()
            .filter(cm -> cm.getPosicion() != null && cm.getPosicion() > posicionEliminada)
            .min(Comparator.comparing(CategoriaMision::getPosicion))
            .map(CategoriaMision::getMision)
            .orElse(null);
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