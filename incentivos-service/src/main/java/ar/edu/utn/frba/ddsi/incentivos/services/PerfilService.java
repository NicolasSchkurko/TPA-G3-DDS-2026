package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ResultadoLotePerfilesDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Alta, consulta y edición de perfiles de donante, y aplicación del progreso de las misiones.
 * Carga, delega en el agregado {@code Perfil} y guarda.
 */
@Slf4j
@Service
public class PerfilService {
    /** Reintentos de una donación que perdió la carrera de concurrencia. */
    private static final int MAX_INTENTOS_CONCURRENCIA = 3;

    /** Perfiles por bloque en la pasada de constancia: chico para el heap, grande para amortizar transacciones. */
    private static final int TAMANO_BLOQUE_CONSTANCIA = 500;

    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioDonaciones repositorioDonaciones;

    /** Inyectado a mano: el reintento necesita abrir una transacción nueva sin pasar por el proxy. */
    private final TransactionTemplate transactionTemplate;

    private final ValidadorAdmin validadorAdmin;

    public PerfilService(RepositorioPerfiles repositorioPerfiles,
                         RepositorioCategorias repositorioCategorias,
                         RepositorioDonaciones repositorioDonaciones,
                         TransactionTemplate transactionTemplate,
                         ValidadorAdmin validadorAdmin) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioDonaciones = repositorioDonaciones;
        this.transactionTemplate = transactionTemplate;
        this.validadorAdmin = validadorAdmin;
    }

    /**
     * Recalcula la racha de los perfiles que están en una misión con constancia. Lo llama el
     * scheduler.
     */
    public void evaluarConstanciaPerfiles() {
        Pageable corte = PageRequest.of(0, TAMANO_BLOQUE_CONSTANCIA, Sort.by("idUsuario"));
        int bloque = 0;

        while (true) {
            // La consulta y el recálculo van en la MISMA transacción del bloque: fuera de ella
            // los perfiles llegan desligados y tocar valoresObservados (LAZY) lanza
            // LazyInitializationException.
            Pageable pagina = corte; // efectivamente final para el lambda
            int procesados = transactionTemplate.execute(estado -> {
                List<Perfil> perfilesDelBloque = repositorioPerfiles
                        .buscarPerfilesConMisionQueRequiereConstancia(pagina)
                        .getContent();
                if (perfilesDelBloque.isEmpty()) {
                    return 0;
                }
                recalcularConstanciaDe(perfilesDelBloque);
                return perfilesDelBloque.size();
            });

            if (procesados == 0) {
                return;
            }
            log.debug("Bloque {} de constancia: {} perfiles recalculados", bloque, procesados);
            if (procesados < TAMANO_BLOQUE_CONSTANCIA) {
                // Última página: no hay más. Sin esto el bucle daría una vuelta de más
                // buscando una página vacía, que es una consulta inútil pero no un bug.
                return;
            }
            corte = corte.next();
            bloque++;
        }
    }

    /** El cuerpo de un bloque, dentro de su propia transacción. */
    private void recalcularConstanciaDe(List<Perfil> perfiles) {
        perfiles.forEach(perfil -> perfil.verificarProgresoMision(
                repositorioDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
                        perfil.getIdUsuario(),
                        perfil.getProgresoMisionActual().getMision().getIdMision())
        ));

        repositorioPerfiles.saveAll(perfiles);
    }

    /**
     * Crea el perfil de un donante con su categoría base y primera misión.
     * {@code @Transactional} es necesario porque {@code primeraMision()} toca una colección LAZY.
     */
    @Transactional
    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        // Se chequea antes de armar nada.
        if (repositorioPerfiles.existsById(dto.getIdUsuario())) {
            throw new PerfilExistenteException(dto.getIdUsuario());
        }
        return crearPerfilNuevo(dto);
    }

    /**
     * Alta en lote: la importación CSV de donaciones manda hasta 500 perfiles por llamada.
     * Un perfil que ya existe se saltea (reintentar una importación parcial es idempotente) y
     * una fila rota no tumba el resto: el motivo queda en {@code errores}.
     */
    @Transactional
    public ResultadoLotePerfilesDTO crearPerfilesEnLote(List<PerfilDonanteDTO> perfiles) {
        int creados = 0;
        int yaExistian = 0;
        List<String> errores = new ArrayList<>();

        for (PerfilDonanteDTO perfil : perfiles) {
            try {
                if (repositorioPerfiles.existsById(perfil.getIdUsuario())) {
                    yaExistian++;
                    continue;
                }
                crearPerfilNuevo(perfil);
                creados++;
            } catch (Exception e) {
                errores.add(perfil.getIdUsuario() + ": " + e.getMessage());
            }
        }

        return new ResultadoLotePerfilesDTO(creados, yaExistian, errores);
    }

    private PerfilDTO crearPerfilNuevo(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

        Categoria categoriaBase = repositorioCategorias.obtenerCategoriaBase()
                .orElseThrow(() -> new CategoriaBaseInexistenteException(
                        "No existe la categoría base configurada"));

        // El agregado arma su propio estado inicial: categoría y primera misión.
        nuevo.iniciarEn(categoriaBase);

        Perfil guardado = repositorioPerfiles.save(nuevo);
        repositorioPerfiles.flush();
        return convertirPerfilADTO(guardado);
    }

    /** El perfil completo de un donante, con su categoría y su misión en curso. */
    @Transactional(readOnly = true)
    public PerfilDTO buscarPorIdUsuario(UUID idUsuario) {
        Perfil p = repositorioPerfiles.findById(idUsuario)
                                      .orElseThrow(() -> new InexistenteException(
                                          "No existe un perfil para el usuario " + idUsuario
                                      ));

        return convertirPerfilADTO(p);
    }

    /**
     * Las insignias que ya obtuvo un donante, paginadas. Se consulta sobre
     * {@code InsigniaObtenida} porque el orden es por fecha de obtención.
     */
    @Transactional(readOnly = true)
    public Page<InsigniaDTO> obtenerInsigniasPorIdUsuario(UUID idUsuario, Pageable pageable) {
        if (!repositorioPerfiles.existsById(idUsuario)) {
            throw new InexistenteException();
        }

        return repositorioPerfiles.paginaInsigniasPorIdUsuario(idUsuario, pageable)
                                  .map(obtenida -> convertirInsigniaADTO(obtenida.getInsignia()));
    }

    /**
     * La misión que el donante tiene en curso, con cuánto lleva recorrido. Un perfil recién
     * creado ya tiene misión, así que sin misión es 404.
     */
    @Transactional(readOnly = true)
    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        ProgresoMision progreso = repositorioPerfiles.obtenerProgresoMisionPorIdUsuario(idUsuario)
                                                .orElseThrow(InexistenteException::new);
        return convertirProgresoMisionADTO(progreso);
    }

    /**
     * La vista pública del perfil: solo nombre de usuario y categoría, porque la ruta es
     * {@code permitAll()}. Sin categoría devuelve {@code null}, no 404.
     */
    @Transactional(readOnly = true)
    public PerfilPublicoDTO obtenerPerfilPublico(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.findById(idUsuario)
                                           .orElseThrow(InexistenteException::new);

        Categoria categoria = perfil.getCategoriaActual();

        return new PerfilPublicoDTO(
                perfil.getNombreUsuario(),
                categoria == null ? null : categoria.getNombre()
        );
    }

    // ========== ACTUALIZAR ==========

    /**
     * Registra el impacto de una donación sobre el perfil del donante. Es idempotente por
     * {@code ImpactoDonacion.idDonacion}.
     */
    public boolean actualizarPerfilImpacto(UUID idUsuario, ImpactoDonacionDTO dto) {
        OptimisticLockingFailureException ultimaFalla = null;

        for (int intento = 1; intento <= MAX_INTENTOS_CONCURRENCIA; intento++) {
            try {
                // TransactionTemplate para arrancar una transacción nueva en cada reintento:
                // la vuelta perdedora ya quedó marcada como rollback.
                Boolean resultado = transactionTemplate.execute(estado ->
                        aplicarImpacto(idUsuario, dto));
                return Boolean.TRUE.equals(resultado);

            } catch (OptimisticLockingFailureException excepcion) {
                ultimaFalla = excepcion;
                log.warn("Carrera de concurrencia al aplicar la donacion {} de {}; "
                                + "intento {} de {}",
                        dto.getIdDonacion(), idUsuario, intento, MAX_INTENTOS_CONCURRENCIA);
            }
        }

        // Se agotaron los intentos: el handler traduce la excepción a 409.
        throw ultimaFalla;
    }

    /**
     * Aplica el impacto dentro de la transacción abierta por
     * {@link #actualizarPerfilImpacto}. El reintento es seguro: la fila no se guardó, así que
     * el {@code findById} no la encuentra.
     */
    private boolean aplicarImpacto(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            throw new DatosInvalidosException("El ID del usuario no puede ser nulo");
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);

        Optional<ImpactoDonacion> yaProcesada =
                repositorioDonaciones.findById(donacion.getIdDonacion());

        if (yaProcesada.isPresent()) {
            log.info("Donación {} de {} repetida: se devuelve el resultado guardado sin "
                    + "reprocesar", donacion.getIdDonacion(), idUsuario);
            return Boolean.TRUE.equals(yaProcesada.get().getCompletMision());
        }

        Perfil p = repositorioPerfiles.findById(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        boolean perfilActualizado = this.progresarPerfil(p, donacion);
        // Se guarda para poder repetir la misma respuesta ante un reintento.
        donacion.registrarSiCompletoMision(perfilActualizado);

        repositorioPerfiles.save(p);
        // El flush va antes de guardar la donación: así el fallo de concurrencia se detecta
        // antes de insertar la fila.
        repositorioPerfiles.flush();
        repositorioDonaciones.save(donacion);

        return perfilActualizado;
    }

    /**
     * Aplica una donación al perfil y, si completó la misión, le pasa la siguiente.
     *
     * @return {@code true} si completó la misión; se guarda en la fila para repetirlo ante un
     *         reintento.
     */
    private boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
        List<ImpactoDonacion> donaciones = List.of();
        Mision misionActual = perfil.getProgresoMisionActual() == null
                              ? null
                              : perfil.getProgresoMisionActual().getMision();

        if (misionActual != null) {
            donaciones = repositorioDonaciones
                .findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
                    perfil.getIdUsuario(),
                    misionActual.getIdMision());
        }

        boolean misionCompletada = perfil.progresarMision(donacion, donaciones);
        if (!misionCompletada || misionActual == null) {
            return misionCompletada;
        }

        this.asignarSiguienteMision(perfil, misionActual);
        return true;
    }

    /**
     * Le pasa al donante la siguiente misión de su secuencia, de la misma categoría o de la
     * siguiente. No llama a servicios externos: el listener resuelve el contacto en
     * {@code AFTER_COMMIT}.
     */
    private void asignarSiguienteMision(Perfil perfil, Mision misionCompletada) {
        Categoria categoriaActual = perfil.getCategoriaActual();
        if (categoriaActual == null) {
            perfil.finalizarSecuencia();
            return;
        }

        Mision siguienteMision = categoriaActual.siguienteMision(misionCompletada);
        if (siguienteMision != null) {
            perfil.cambiarMision(siguienteMision, misionCompletada);
            return;
        }

        Categoria siguienteCategoria = repositorioCategorias
            .obtenerCategoriaSiguiente(categoriaActual);

        if (siguienteCategoria != null) {
            perfil.cambiarCategoria(siguienteCategoria, categoriaActual, misionCompletada);
            return;
        }

        // Se agotó la secuencia: no hay más categorías ni más misiones que ofrecerle.
        perfil.finalizarSecuencia();
    }

    /**
     * Cambia el nombre de usuario del donante. Un nombre vacío se ignora. Exige
     * administrador.
     */
    @Transactional
    public PerfilDTO actualizarDatosPerfil(UUID idUsuario, UUID idAdmin, PerfilDTO dto) {
        validadorAdmin.verificarPermisos(idAdmin);

        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        Perfil p = repositorioPerfiles.findById(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isEmpty()) {
            p.cambiarNombre(dto.getNombreUsuario());
        }

        Perfil actualizado = repositorioPerfiles.save(p);

        return convertirPerfilADTO(actualizado);
    }

    // ========== CONVERTIDORES ==========

    /** Proyecta el perfil al DTO de respuesta, centralizando los null checks. */
    public PerfilDTO convertirPerfilADTO(Perfil perfil) {
        Categoria categoria = perfil.getCategoriaActual();
        Set<InsigniaObtenida> insignias = perfil.getInsigniasObtenidas();
        ProgresoMision progreso = perfil.getProgresoMisionActual();
        Mision mision = progreso == null ? null : progreso.getMision();

        return new PerfilDTO(
            perfil.getNombreUsuario(),
            categoria == null ? null : categoria.getNombre(),
            insignias == null
                ? List.of()
                : insignias.stream().map(io -> io.getInsignia().getNombre()).toList(),
            mision == null ? null : mision.getNombreMision()
        );
    }

    /** Traduce el DTO de la donación a la entidad. */
    public ImpactoDonacion convertirDTO(UUID idUsuario, ImpactoDonacionDTO donacion) {
        return new ImpactoDonacion(
            donacion.getIdDonacion(),
            idUsuario,
            donacion.getEntidadBeneficiaria(),
            donacion.getCantidadBienes(),
            donacion.getFechaEntrega(),
            donacion.getCategoria(),
            donacion.getSubCategoria(),
            donacion.getEstado());
    }

    /**
     * Arma el DTO de la misión vigente con el avance y la distancia restante al objetivo.
     * Con {@code VALORES_DISTINTOS} el faltante puede llegar a 0 sin estar completa.
     */
    public MisionPerfilDTO convertirProgresoMisionADTO(ProgresoMision progreso) {
        Mision mision = progreso.getMision();
        Integer actual = progreso.getProgreso() == null ? 0 : progreso.getProgreso();

        Operacion operacion = mision.getReglaDeProgreso() == null
                              ? null
                              : mision.getReglaDeProgreso().getOperacion();
        Integer objetivo = operacion == null ? null : operacion.getProgresoObjetivo();

        Integer faltante = objetivo == null
                ? null
                : Math.max(0, objetivo - actual);

        Insignia insignia = mision.getInsigniaObjetivo();

        return new MisionPerfilDTO(
            mision.getNombreMision(),
            mision.getDescripcion(),
            insignia == null ? null : insignia.getNombre(),
            actual,
            objetivo,
            faltante
        );
    }

    /** Traduce la entidad de insignia al DTO que ve el cliente. */
    public InsigniaDTO convertirInsigniaADTO(Insignia insignia) {
        return new InsigniaDTO(
            insignia.getNombre(),
            insignia.getDescripcion(),
            insignia.getUrlImagen()
        );
    }

    // ========== ELIMINAR ==========
    /**
     * Borra el perfil del donante. El 404 va por {@link InexistenteException}. Exige
     * administrador.
     */
    @Transactional
    public void eliminarPerfil(UUID idUsuario, UUID idAdmin) {
        validadorAdmin.verificarPermisos(idAdmin);

        if (!repositorioPerfiles.existsById(idUsuario)) {
            throw new InexistenteException();
        }
        repositorioPerfiles.deleteById(idUsuario);
    }
}
