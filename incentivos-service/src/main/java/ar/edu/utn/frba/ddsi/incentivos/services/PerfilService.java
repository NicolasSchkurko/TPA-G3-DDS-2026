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
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
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

    /** Espera mínima y máxima antes de reintentar una carrera de concurrencia. */
    private static final long RETRASO_MINIMO_MS = 20;
    private static final long RETRASO_MAXIMO_MS = 100;

    /** Perfiles por bloque en la pasada de constancia: chico para el heap, grande para amortizar transacciones. */
    private static final int TAMANO_BLOQUE_CONSTANCIA = 500;

    /** Perfiles por transacción en el alta en lote. */
    private static final int TAMANO_BLOQUE_LOTE = 100;

    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioDonaciones repositorioDonaciones;

    /** Inyectado a mano: el reintento necesita abrir una transacción nueva sin pasar por el proxy. */
    private final TransactionTemplate transactionTemplate;


    public PerfilService(RepositorioPerfiles repositorioPerfiles,
                         RepositorioCategorias repositorioCategorias,
                         RepositorioDonaciones repositorioDonaciones,
                         TransactionTemplate transactionTemplate
    ) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioDonaciones = repositorioDonaciones;
        this.transactionTemplate = transactionTemplate;
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
        List<UUID> idsUsuarios = perfiles.stream().map(Perfil::getIdUsuario).toList();
        Set<UUID> misiones = perfiles.stream()
                .map(perfil -> perfil.getProgresoMisionActual().getMision().getIdMision())
                .collect(Collectors.toSet());

        // Una sola consulta por bloque: antes era una por perfil.
        Map<UUID, List<ImpactoDonacion>> donacionesPorUsuario = repositorioDonaciones
                .buscarDonacionesQueProgresaronDePerfiles(idsUsuarios, misiones)
                .stream()
                .collect(Collectors.groupingBy(ImpactoDonacion::getIdUsuario));

        for (Perfil perfil : perfiles) {
            UUID idMision = perfil.getProgresoMisionActual().getMision().getIdMision();
            List<ImpactoDonacion> donacionesDelPerfil =
                    donacionesPorUsuario.getOrDefault(perfil.getIdUsuario(), List.of())
                            .stream()
                            .filter(donacion -> Objects.equals(
                                    donacion.getIdMision(), idMision))
                            .toList();

            perfil.verificarProgresoMision(donacionesDelPerfil);
        }

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
     * La categoría base se resuelve una sola vez y los perfiles se persisten con
     * {@code saveAll} en bloques acotados; si un bloque falla, se reintenta ítem por ítem
     * para aislar la fila rota.
     */
    public ResultadoLotePerfilesDTO crearPerfilesEnLote(List<PerfilDonanteDTO> perfiles) {
        if (perfiles.isEmpty()) {
            return new ResultadoLotePerfilesDTO(0, 0, List.of());
        }

        Optional<Categoria> categoriaBase = repositorioCategorias.obtenerCategoriaBase();
        if (categoriaBase.isEmpty()) {
            String mensaje = "No existe la categoría base configurada";
            List<String> errores = perfiles.stream()
                    .map(perfil -> perfil.getIdUsuario() + ": " + mensaje)
                    .toList();
            return new ResultadoLotePerfilesDTO(0, 0, errores);
        }

        int creados = 0;
        int yaExistian = 0;
        List<String> errores = new ArrayList<>();

        for (int desde = 0; desde < perfiles.size(); desde += TAMANO_BLOQUE_LOTE) {
            List<PerfilDonanteDTO> bloque = perfiles.subList(
                    desde, Math.min(desde + TAMANO_BLOQUE_LOTE, perfiles.size()));

            ResultadoBloque resultado =
                    procesarBloque(bloque, categoriaBase.get());
            creados += resultado.creados();
            yaExistian += resultado.yaExistian();
            errores.addAll(resultado.errores());
        }

        return new ResultadoLotePerfilesDTO(creados, yaExistian, errores);
    }

    /** Persiste un bloque con saveAll; si falla, reintenta ítem por ítem para aislar el error. */
    private ResultadoBloque procesarBloque(List<PerfilDonanteDTO> bloque,
                                           Categoria categoriaBase) {
        List<UUID> ids = bloque.stream().map(PerfilDonanteDTO::getIdUsuario).toList();
        Set<UUID> existentes = new HashSet<>(
                repositorioPerfiles.buscarIdsExistentes(ids));

        int yaExistian = 0;
        Set<UUID> idsDelBloque = new HashSet<>();
        List<PerfilDonanteDTO> nuevos = new ArrayList<>();
        for (PerfilDonanteDTO perfil : bloque) {
            if (existentes.contains(perfil.getIdUsuario())
                    || !idsDelBloque.add(perfil.getIdUsuario())) {
                yaExistian++;
            } else {
                nuevos.add(perfil);
            }
        }

        if (nuevos.isEmpty()) {
            return new ResultadoBloque(0, yaExistian, List.of());
        }

        try {
            Integer creados = transactionTemplate.execute(estado -> {
                List<Perfil> perfilesNuevos = nuevos.stream()
                        .map(perfil -> construirPerfil(perfil, categoriaBase))
                        .toList();
                repositorioPerfiles.saveAll(perfilesNuevos);
                repositorioPerfiles.flush();
                return perfilesNuevos.size();
            });
            return new ResultadoBloque(
                    creados == null ? 0 : creados, yaExistian, List.of());
        } catch (Exception bloqueFallido) {
            log.warn("Fallo el bloque de perfiles; se reintenta ítem por ítem: {}",
                    bloqueFallido.getMessage());
            return reintentarUnoPorUno(nuevos, categoriaBase, yaExistian);
        }
    }

    private ResultadoBloque reintentarUnoPorUno(List<PerfilDonanteDTO> nuevos,
                                                Categoria categoriaBase,
                                                int yaExistian) {
        int creados = 0;
        List<String> errores = new ArrayList<>();

        for (PerfilDonanteDTO perfil : nuevos) {
            try {
                transactionTemplate.executeWithoutResult(estado -> {
                    repositorioPerfiles.save(construirPerfil(perfil, categoriaBase));
                    repositorioPerfiles.flush();
                });
                creados++;
            } catch (Exception e) {
                log.warn("Fallo al persistir perfil en lote {}: {}",
                        perfil.getIdUsuario(), e.getMessage());
                errores.add(formatearError(perfil.getIdUsuario(), e));
            }
        }

        return new ResultadoBloque(creados, yaExistian, errores);
    }

    private record ResultadoBloque(int creados, int yaExistian, List<String> errores) {
    }

    private String formatearError(UUID idUsuario, Exception e) {
        if (e instanceof DataIntegrityViolationException) {
            return idUsuario + ": Error de integridad de datos al persistir el perfil";
        }
        String msg = e.getMessage();
        return idUsuario + ": " + (msg != null && !msg.isBlank() ? msg : e.getClass().getSimpleName());
    }

    private PerfilDTO crearPerfilNuevo(PerfilDonanteDTO dto) {
        Categoria categoriaBase = repositorioCategorias.obtenerCategoriaBase()
                .orElseThrow(() -> new CategoriaBaseInexistenteException(
                        "No existe la categoría base configurada"));

        Perfil guardado = repositorioPerfiles.save(construirPerfil(dto, categoriaBase));
        repositorioPerfiles.flush();
        return convertirPerfilADTO(guardado);
    }

    private Perfil construirPerfil(PerfilDonanteDTO dto, Categoria categoriaBase) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());
        // El agregado arma su propio estado inicial: categoría y primera misión.
        nuevo.iniciarEn(categoriaBase);
        return nuevo;
    }

    /** El perfil completo de un donante, con su categoría y su misión en curso. */
    @Transactional(readOnly = true)
    public PerfilDTO buscarPorIdUsuario(UUID idUsuario) {
        Perfil p = repositorioPerfiles.findById(idUsuario)
                                      .filter(Perfil::isActivo)
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
        repositorioPerfiles.findById(idUsuario)
                .filter(Perfil::isActivo)
                .orElseThrow(InexistenteException::new);

        return repositorioPerfiles.paginaInsigniasPorIdUsuario(idUsuario, pageable)
                                  .map(obtenida -> convertirInsigniaADTO(obtenida.getInsignia()));
    }

    /**
     * La misión que el donante tiene en curso, con cuánto lleva recorrido. Un perfil recién
     * creado ya tiene misión, así que sin misión es 404.
     */
    @Transactional(readOnly = true)
    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        Perfil p = repositorioPerfiles.findById(idUsuario)
                .filter(Perfil::isActivo)
                .orElseThrow(InexistenteException::new);

        ProgresoMision progreso = p.getProgresoMisionActual();
        if (progreso == null) {
            throw new InexistenteException();
        }
        return convertirProgresoMisionADTO(progreso);
    }

    /**
     * La vista pública del perfil: solo nombre de usuario y categoría, porque la ruta es
     * {@code permitAll()}. Sin categoría devuelve {@code null}, no 404.
     */
    @Transactional(readOnly = true)
    public PerfilPublicoDTO obtenerPerfilPublico(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.findById(idUsuario)
                                           .filter(Perfil::isActivo)
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

                if (intento < MAX_INTENTOS_CONCURRENCIA) {
                    esperarReintento(intento);
                }
            }
        }

        // Se agotaron los intentos: el handler traduce la excepción a 409.
        throw ultimaFalla;
    }

    /** Espera con backoff exponencial y jitter antes de reintentar una carrera. */
    void esperarReintento(int intento) {
        long retraso = retrasoMilisegundos(intento);
        try {
            Thread.sleep(retraso);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Se interrumpió el reintento de la donación", e);
        }
    }

    /** Retraso del intento indicado, acotado entre {@code RETRASO_MINIMO_MS} y el máximo. */
    static long retrasoMilisegundos(int intento) {
        long maximo = Math.min(RETRASO_MAXIMO_MS, RETRASO_MINIMO_MS * (1L << intento));
        return ThreadLocalRandom.current().nextLong(RETRASO_MINIMO_MS, maximo + 1);
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

        Perfil p = repositorioPerfiles.findByIdParaDonacion(idUsuario)
                                      .filter(Perfil::isActivo)
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

        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        Perfil p = repositorioPerfiles.findById(idUsuario)
                                      .filter(Perfil::isActivo)
                                      .orElseThrow(InexistenteException::new);

        if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isBlank()) {
            p.cambiarNombre(dto.getNombreUsuario().trim());
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
     * Da de baja el perfil del donante de forma lógica (soft delete) y minimiza sus datos.
     * Preserva intacto el historial en ImpactoDonacion para salvaguardar las métricas contables
     * y los snapshots de ranking.
     * El 404 va por {@link InexistenteException}. Exige administrador.
     */
    @Transactional
    public void eliminarPerfil(UUID idUsuario, UUID idAdmin) {

        Perfil perfil = repositorioPerfiles.findById(idUsuario)
                .filter(Perfil::isActivo)
                .orElseThrow(InexistenteException::new);

        darDeBajaPerfil(perfil);
    }

    /**
     * Variante sin autenticación para el borrado en cascada que dispara donaciones-service al
     * eliminar un donante: la baja ya la autorizó donaciones-service, así que exigir
     * una autorización de admin acá solo duplicaría (mal) esa validación. Idempotente -- si el donante
     * nunca llegó a donar o ya fue dado de baja, no falla -- para que donaciones-service no
     * tenga que distinguir "ya no tenía perfil" de "se dio de baja bien".
     */
    @Transactional
    public void eliminarPerfilPorBajaDeDonante(UUID idUsuario) {
        Optional<Perfil> opt = repositorioPerfiles.findById(idUsuario);
        if (opt.isEmpty() || !opt.get().isActivo()) {
            return;
        }
        darDeBajaPerfil(opt.get());
    }

    /**
     * Variante sin autenticación para propagar cambios de nombre de donante desde donaciones-service.
     * Idempotente: si el donante no existe o está inactivo, no falla.
     */
    @Transactional
    public void actualizarPerfilPorModificacionDeDonante(UUID idUsuario, PerfilDonanteDTO dto) {
        if (idUsuario == null || dto == null) return;
        Optional<Perfil> opt = repositorioPerfiles.findById(idUsuario);
        if (opt.isEmpty() || !opt.get().isActivo()) return;

        Perfil perfil = opt.get();
        if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isBlank()) {
            perfil.cambiarNombre(dto.getNombreUsuario().trim());
            repositorioPerfiles.save(perfil);
            log.debug("Perfil {} actualizado con nuevo nombreUsuario='{}'", idUsuario, perfil.getNombreUsuario());
        }
    }

    /**
     * Aplica la baja lógica y minimización de datos en Perfil.
     * Las filas de ImpactoDonacion se preservan intactas en la base de datos.
     */
    private void darDeBajaPerfil(Perfil perfil) {
        perfil.darDeBaja();
        repositorioPerfiles.save(perfil);
    }
}
