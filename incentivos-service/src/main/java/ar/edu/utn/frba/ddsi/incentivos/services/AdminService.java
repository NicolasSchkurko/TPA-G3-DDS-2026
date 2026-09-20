package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.ValoresDistintos;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorMision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorSecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioMisiones repositorioMisiones;
    private final GestorSecuenciaCategoria gestorSecuenciaCategoria;
    private final GestorMision gestorMisiones;
    private final DonacionClient donacionClient;
    private final MisionFactory misionFactory;

    public AdminService(RepositorioCategorias repositorioCategorias,
                        RepositorioMisiones repositorioMisiones,
                        GestorSecuenciaCategoria gestorSecuenciaCategoria,
                        GestorMision gestorMision,
                        DonacionClient donacionClient,
                        MisionFactory misionFactory) {
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioMisiones = repositorioMisiones;
        this.gestorSecuenciaCategoria = gestorSecuenciaCategoria;
        this.gestorMisiones = gestorMision;
        this.donacionClient = donacionClient;
        this.misionFactory = misionFactory;

        inicializarCategoriasBase();
    }

    @Transactional
    protected void inicializarCategoriasBase() {
        if (repositorioCategorias.count() == 0) {
            Mision mision1 = misionFactory.crearMision(null, "Primera donación", "Realiza tu primera donación para empezar a colaborar.", "Primer paso", null, AtributoImpacto.ESTADO, misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA"));
            Mision mision2 = misionFactory.crearMision(null, "Segunda donación", "Realiza tu segunda donación.", "Sigo ayudando", null, AtributoImpacto.ESTADO, misionFactory.crearOperacion("COINCIDENCIAS", 1, null, "ENTREGADA"));
            Mision mision3 = misionFactory.crearMision(null, "Supera tus límites", "Dona más de 10 bienes", "Rompiendo los límites", null, AtributoImpacto.CANTIDAD_BIENES, misionFactory.crearOperacion("SUPERA_CANTIDAD", 1, 10, "ENTREGADA"));

            repositorioMisiones.saveAll(List.of(mision1, mision2, mision3));

            Categoria colaborador = new Categoria("Colaborador", null, 1, new ArrayList<>());
            Categoria sostenedor = new Categoria("Sostenedor", null, 2, new ArrayList<>());
            Categoria transformador = new Categoria("Transformador", null, 3, new ArrayList<>());

            colaborador.agregarMision(mision1);
            sostenedor.agregarMision(mision2);
            sostenedor.agregarMision(mision3);

            repositorioCategorias.saveAll(List.of(colaborador, sostenedor, transformador));
        }
    }

    private void verificarPermisos(UUID idAdmin) {
        if (!donacionClient.verificarAdmin(idAdmin)) {
            throw new SecurityException("El usuario no tiene permisos de administrador o no existe.");
        }
    }

    // ========== CATEGORÍAS - GET ==========
    public List<CategoriaDTO> obtenerCategorias(UUID idAdmin) {
        verificarPermisos(idAdmin);

        List<Categoria> categorias = repositorioCategorias.obtenerTodas();
        return categorias.stream()
                         .map(this::categoriaToDTO)
                         .toList();
    }

    public CategoriaDTO obtenerCategoriaPorId(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        Categoria categoria = repositorioCategorias.obtenerPorId(id);
        return categoria != null ? categoriaToDTO(categoria) : null;
    }

    // ========== CATEGORÍAS - CREATE ==========
    @Transactional
    public CategoriaDTO agregarCategoria(UUID idAdmin, CategoriaDTO dto) {
        verificarPermisos(idAdmin);

        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());

        Categoria categoria = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );

        gestorSecuenciaCategoria.desplazarParaCrear(categoria.getPosicionSecuencia());
        Categoria categoriaCreada = repositorioCategorias.save(categoria);

        return categoriaCreada != null ? categoriaToDTO(categoriaCreada) : null;
    }

    // ========== CATEGORÍAS - UPDATE ==========
    @Transactional
    public CategoriaDTO actualizarCategoria(UUID idAdmin, UUID id, CategoriaDTO dto) {
        verificarPermisos(idAdmin);

        List<Mision> misiones = gestorMisiones.conseguirMisiones(dto.getMisiones());
        Categoria categoriaModificada = new Categoria(
            dto.getNombre(),
            idAdmin,
            dto.getPosicionSecuencia(),
            misiones
        );

        Categoria actualizada = repositorioCategorias.findById(id).map(categoriaActual -> {
            if (categoriaModificada.getPosicionSecuencia() != null) {
                gestorSecuenciaCategoria.desplazarParaActualizar(
                    categoriaActual.getPosicionSecuencia(),
                    categoriaModificada.getPosicionSecuencia(),
                    repositorioCategorias.count()
                );
                categoriaActual.setPosicionSecuencia(categoriaModificada.getPosicionSecuencia());
            }

            categoriaActual.copiar(categoriaModificada);
            return repositorioCategorias.save(categoriaActual);
        }).orElse(null);

        return actualizada != null ? categoriaToDTO(actualizada) : null;
    }

    // ========== CATEGORÍAS - DELETE ==========
    @Transactional
    public List<CategoriaDTO> eliminarCategoria(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        List<Categoria> categoriasResultantes = repositorioCategorias.findById(id).map(cat -> {
            Integer posicionLiberada = cat.getPosicionSecuencia();
            repositorioCategorias.delete(cat);

            gestorSecuenciaCategoria.desplazarParaEliminar(posicionLiberada);

            return repositorioCategorias.obtenerTodas();
        }).orElse(List.of());

        return categoriasResultantes.stream()
                                    .map(this::categoriaToDTO)
                                    .toList();
    }

    // ========== MISIONES - GET ==========
    public List<MisionDTO> obtenerMisiones(UUID idAdmin) {
        verificarPermisos(idAdmin);

        List<Mision> misiones = gestorMisiones.obtenerTodas();
        return misiones.stream()
                       .map(this::misionToDTO)
                       .toList();
    }

    public MisionDTO obtenerMisionPorId(UUID idAdmin, UUID id) {
        verificarPermisos(idAdmin);

        Mision mision = gestorMisiones.obtenerPorId(id);
        return mision != null ? misionToDTO(mision) : null;
    }

    // ========== MISIONES - CREATE ==========
    @Transactional
    public MisionDTO crearMision(UUID idAdmin, MisionDTO nuevaMision) {
        verificarPermisos(idAdmin);

        ReglaDTO reglaDTO = nuevaMision.getRegla();
        ConstanciaDTO constanciaDTO = reglaDTO.getConstancia();
        OperacionDTO operacionDTO = reglaDTO.getOperacion();
        String atributo = reglaDTO.getAtributo();

        Mision m = gestorMisiones.crearMision(
            idAdmin,
            nuevaMision.getNombreMision(),
            nuevaMision.getDescripcion(),
            nuevaMision.getInsigniaObjetivo(),
            gestorMisiones.conseguirConstancia(
                constanciaDTO.getCantidad(),
                constanciaDTO.getUnidadTiempo()
            ),
            atributo,
            gestorMisiones.conseguirOperacion(
                operacionDTO.getTipoOperacion(),
                operacionDTO.getProgresoObjetivo(),
                operacionDTO.getCantidad(),
                operacionDTO.getValorEsperado()
            )
        );
        return misionToDTO(m);
    }

    // ========== MISIONES - UPDATE ==========
    @Transactional
    public MisionDTO actualizarMision(UUID idAdmin, UUID idMision, MisionDTO dto) {
        verificarPermisos(idAdmin);

        ReglaDTO reglaDTO = dto.getRegla();
        ConstanciaDTO constanciaDTO = reglaDTO.getConstancia();
        OperacionDTO operacionDTO = reglaDTO.getOperacion();
        String atributo = reglaDTO.getAtributo();

        Mision mision = gestorMisiones.crearMision(
            idAdmin,
            dto.getNombreMision(),
            dto.getDescripcion(),
            dto.getInsigniaObjetivo(),
            gestorMisiones.conseguirConstancia(
                constanciaDTO.getCantidad(),
                constanciaDTO.getUnidadTiempo()
            ),
            atributo,
            gestorMisiones.conseguirOperacion(
                operacionDTO.getTipoOperacion(),
                operacionDTO.getProgresoObjetivo(),
                operacionDTO.getCantidad(),
                operacionDTO.getValorEsperado()
            )
        );

        mision.setIdMision(idMision);

        Mision actualizada = gestorMisiones.actualizarMision(mision);
        return actualizada != null ? misionToDTO(actualizada) : null;
    }

    // ========== MISIONES - DELETE ==========
    @Transactional
    public MisionDTO eliminarMision(UUID idAdmin, UUID idMision) {
        verificarPermisos(idAdmin);
        Mision mision = gestorMisiones.eliminarMision(idMision);
        return misionToDTO(mision);
    }

    // ========== CONVERTIDORES ==========
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