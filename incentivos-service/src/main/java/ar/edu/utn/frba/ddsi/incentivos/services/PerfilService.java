package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Alta, consulta y edición de perfiles de donante, y aplicación del progreso de las
 * misiones.
 *
 * <p>Es el servicio que concentra las reglas de progresión: cuándo una donación suma, cuándo
 * la misión se completa, cuándo al donante le toca la siguiente y cuándo sube de categoría.
 * Casi todo eso vive en el agregado {@code Perfil}, no acá: este servicio carga, delega y
 * guarda.
 */
@Slf4j
@Service
public class PerfilService {
    /**
     * Cuántas veces se reintenta la aplicación de una donación que perdió la carrera de
     * concurrencia (punto 36).
     *
     * <p>Tres porque la carrera se resuelve en milisegundos: dos donaciones que
     * terminan a la vez, una de las dos gana y la otra reintenta contra una base
     * que ya no tiene a nadie escribiendo. Con más intentos se empieza a esperar
     * por bloqueos de fila que no se van a resolver, y con menos se convierte una
     * carrera puntual en un error para el donante.
     */
    private static final int MAX_INTENTOS_CONCURRENCIA = 3;

    /**
     * Cuántos perfiles recalcula por bloque la pasada de constancia (punto 22).
     *
     * <p>Quinientos es un número arbitrario, elegido por un criterio concreto: es
     * suficientemente chico para que el bloque entre cómodo en el heap por mucho que
     * crezca la base, y suficientemente grande para que el costo de abrir una transacción
     * por bloque sea despreciable frente al trabajo de la consulta de donaciones. Con
     * bloques de cinco, una pasada de 10.000 perfiles abriría 2.000 transacciones; con
     * bloques de 50.000, el pico de memoria volvería a ser el problema original.
     */
    private static final int TAMANO_BLOQUE_CONSTANCIA = 500;

    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioDonaciones repositorioDonaciones;

    /**
     * Se inyecta a mano y no por anotacion justamente porque hace falta sin proxy: el
     * reintento de concurrencia tiene que abrir una transaccion nueva desde adentro de
     * esta misma clase.
     */
    private final TransactionTemplate transactionTemplate;

    public PerfilService(RepositorioPerfiles repositorioPerfiles,
                         RepositorioCategorias repositorioCategorias,
                         RepositorioDonaciones repositorioDonaciones,
                         TransactionTemplate transactionTemplate) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioDonaciones = repositorioDonaciones;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Recalcula la racha de todos los que están en una misión con constancia.
     *
     * <p>Lo llama el scheduler: la racha caduca por el paso del tiempo, no por una
     * donación, así que sin esta pasada un donante que dejó de donar queda con el avance
     * congelado y la misión nunca aparece como pendiente.
     *
     * <p><b>Va por bloques y no de una sola vez</b> (punto 22). Antes se traían todos los
     * perfiles con constancia de golpe y se guardaban todos juntos: con 10.000 perfiles eran
     * 10.000 objetos {@code Perfil}, cada uno con su progreso y su misión vivos, más la
     * sesión de Hibernate conteniendo todo eso. Ahora se procesa de a bloques y cada bloque
     * se guarda y se libera antes de pedir el siguiente, así que el pico de memoria no
     * depende del tamaño de la base.
     *
     * <p><b>El bloque es una transacción propia</b>, y no un recorte dentro de la misma
     * transacción grande. Es lo que hace que libere memoria de verdad: con un único
     * {@code @Transactional} de principio a fin, la sesión sigue acumulando las entidades
     * ya procesadas y paginar el SELECT no evita el crecimiento del persistence context.
     *
     * <p>Sobre las consultas: la de los perfiles trae la misión y la regla en la misma ida
     * (ver {@code buscarPerfilesConMisionQueRequiereConstancia}), así que no hay N+1 de esas.
     * La de las donaciones de cada perfil sigue siendo una por donante, porque cada uno
     * necesita las de <em>su</em> misión y una consulta con las dos colecciones cruzadas da
     * un producto cartesiano. Dejarla así es un compromiso consciente, no un descuido:
     * cuando haya datos reales se verá si el número de consultas justifica una consulta
     * agregada por usuario y misión.
     *
     * <p><b>El corte es por offset y por eso el orden importa.</b> Paginar por offset sobre
     * una consulta sin {@code ORDER BY} no es estable: la base puede devolver las mismas
     * filas en órdenes distintos entre consultas, y con eso algunos perfiles se procesan
     * dos veces y otros se saltan sin que ninguna excepción avise. El sort es por
     * {@code idUsuario}, que es único y no cambia durante la pasada —el filtro es "tener una
     * misión con regla de constancia", y recalcular la racha no cambia ni la misión ni la
     * regla, así que el conjunto es estable—.
     */
    public void evaluarConstanciaPerfiles() {
        Pageable corte = PageRequest.of(0, TAMANO_BLOQUE_CONSTANCIA, Sort.by("idUsuario"));
        int bloque = 0;

        while (true) {
            // La consulta y el recálculo van en la MISMA transacción del bloque: fuera de ella
            // los perfiles llegan desligados y tocar valoresObservados (LAZY) lanza
            // LazyInitializationException (punto 6).
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
     * Crea el perfil de un donante y lo deja listo para empezar: categoría base y primera
     * misión.
     *
     * <p>Se verifica que el donante no tenga perfil antes de armar nada, así el error de
     * duplicado no deja un agregado a medio construir.
     *
     * <p><b>La transacción no es opcional (punto 25).</b> Sin ella,
     * {@code findAllByOrderByPosicionSecuenciaAsc()} corre en su propia transacción
     * read-only y devuelve la categoría desligada: la sesión ya se cerró. Como
     * {@code Categoria.categoriaMisiones} es LAZY y {@code open-in-view} está desactivado,
     * {@code primeraMision()} toca una colección sin sesión y falla de una de dos formas: o
     * lanza {@code LazyInitializationException} y el alta responde 500, o —peor— el
     * {@code PersistentBag.isEmpty()} devuelve el tamaño cacheado sin inicializar, devuelve
     * {@code true} en silencio, {@code primeraMision()} da {@code null} y el perfil queda con
     * {@code progresoMisionActual == null} <b>para siempre</b>. Como
     * {@code progresarPerfil} corta en {@code if (misionActual != null)}, ninguna donación
     * posterior de ese donante progresa jamás: no completa misiones, no recibe insignias y
     * nunca aparece en el ranking.
     *
     * <p>La consulta de la categoría base además trae la secuencia de misiones en la misma
     * ida, así que el método no depende del alcance de la transacción para armar el perfil.
     */
    @Transactional
    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        // Se chequea antes de armar nada: si el donante ya existe, no tiene sentido
        // resolver la categoría base ni tocar la base de datos.
        if (repositorioPerfiles.existsByIdUsuario(dto.getIdUsuario())) {
            throw new PerfilExistenteException(dto.getIdUsuario());
        }

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
        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(() -> new InexistenteException(
                                          "No existe un perfil para el usuario " + idUsuario
                                      ));

        return convertirPerfilADTO(p);
    }

    /**
     * Las insignias que ya obtuvo un donante, paginadas.
     *
     * <p>Se consulta sobre {@code InsigniaObtenida} y no sobre la insignia pelada, porque
     * el orden es por fecha de obtención y esa fecha solo existe en la tabla intermedia.
     */
    @Transactional(readOnly = true)
    public Page<InsigniaDTO> obtenerInsigniasPorIdUsuario(UUID idUsuario, Pageable pageable) {
        if (!repositorioPerfiles.existsByIdUsuario(idUsuario)) {
            throw new InexistenteException();
        }

        return repositorioPerfiles.paginaInsigniasPorIdUsuario(idUsuario, pageable)
                                  .map(obtenida -> convertirInsigniaADTO(obtenida.getInsignia()));
    }

    /**
     * La misión que el donante tiene en curso, con cuánto lleva recorrido.
     *
     * <p>Un perfil recién creado ya tiene misión, así que la única forma de que no haya es
     * que el donante no exista, y en ese caso es 404.
     */
    @Transactional(readOnly = true)
    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        ProgresoMision progreso = repositorioPerfiles.obtenerProgresoMisionPorIdUsuario(idUsuario)
                                                .orElseThrow(InexistenteException::new);
        return convertirProgresoMisionADTO(progreso);
    }

    /**
     * La vista publica del perfil: nombre de usuario y nombre de su categoria.
     *
     * <p>Es lo que el enunciado pide que sea visible publicamente (punto 8), y por eso
     * devuelve un {@link PerfilPublicoDTO} con solo esos dos campos en vez del
     * {@code PerfilDTO} completo: la ruta es {@code permitAll()}, asi que lo que viaje
     * en la respuesta queda expuesto.
     *
     * <p>Un perfil sin categoria devuelve {@code nombreCategoria = null} en vez de un 404:
     * el donante existe y su nombre tiene que poder verse igual. Solo falla si el donante
     * no existe.
     */
    @Transactional(readOnly = true)
    public PerfilPublicoDTO obtenerPerfilPublico(UUID idUsuario) {
        Perfil perfil = repositorioPerfiles.findByIdUsuario(idUsuario)
                                           .orElseThrow(InexistenteException::new);

        Categoria categoria = perfil.getCategoriaActual();

        return new PerfilPublicoDTO(
                perfil.getNombreUsuario(),
                categoria == null ? null : categoria.getNombre()
        );
    }

    // ========== ACTUALIZAR ==========

    /**
     * Registra el impacto de una donación sobre el perfil del donante.
     *
     * <p><b>Es idempotente</b> (punto 14). Un reintento del cliente no puede volver a sumar
     * progreso ni otorgar una segunda insignia: si la donación ya se procesó, se devuelve
     * el mismo resultado que se devolvió la primera vez y no se toca nada.
     *
     * <p>Esto importa porque {@code N8nClient} solía relanzar su excepción después del
     * commit (punto 13), lo que dejaba al donante viendo un 500 con la transacción ya
     * confirmada. Con ese 500, cualquier cliente HTTP reintenta, y sin esta guarda cada
     * reintento insertaba una fila nueva y volvía a aplicar la regla.
     *
     * <p>La clave es {@code ImpactoDonacion.idDonacion}, que es el id de la donación en el
     * servicio de origen y además la primary key local. Como es única, la consulta es un
     * {@code findById} y no hace falta comparar el contenido para decidir si es un
     * reintento. El id es obligatorio en el DTO: sin él no hay clave con la que deduplicar,
     * y el 400 es preferible a guardar una fila imposible de deduplicar.
     */
    public boolean actualizarPerfilImpacto(UUID idUsuario, ImpactoDonacionDTO dto) {
        OptimisticLockingFailureException ultimaFalla = null;

        for (int intento = 1; intento <= MAX_INTENTOS_CONCURRENCIA; intento++) {
            try {
                // TransactionTemplate y no @Transactional porque el reintento tiene que
                // arrancar en una transaccion nueva: la de la vuelta perdedora ya esta
                // marcada como rollback y reusarla no serviria de nada. Y tampoco se puede
                // con @Transactional(REQUIRES_NEW) en un metodo privado, porque las
                // llamadas internas no pasan por el proxy de Spring. Es el mismo problema
                // que tiene ValidadorAdmin.verificarPermisos (punto 23).
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

        // Se agotaron los intentos. Sube la excepcion y el handler la traduce a 409, que
        // es un codigo reintentable por definicion: el cliente que lo recibe sabe que
        // puede volver a llamar sin miedo.
        throw ultimaFalla;
    }

    /**
     * Aplica el impacto de una donacion, dentro de la transaccion que abrio el
     * {@link TransactionTemplate} de {@link #actualizarPerfilImpacto}.
     *
     * <p>El reintento es seguro por el punto 14: la fila de la donacion no se llego a
     * guardar cuando se detecta la carrera, asi que al volver a entrar el
     * {@code findById} no la encuentra y el camino idempotente sigue igual. Y si otra
     * transaccion la guardo en el medio, el reintento devuelve ese resultado y sale, que
     * tambien es lo correcto.
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

        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        boolean perfilActualizado = this.progresarPerfil(p, donacion);
        // Se guarda para poder repetir la misma respuesta ante un reintento.
        donacion.registrarSiCompletoMision(perfilActualizado);

        repositorioPerfiles.save(p);
        // El flush va antes de guardar la donacion a proposito: el @Version de Perfil se
        // valida en el UPDATE, y sin forzarlo aca el fallo de concurrencia se detectaria
        // despues de haber insertado la fila de la donacion, con el reintento partiendo de
        // un estado que no espera.
        repositorioPerfiles.flush();
        repositorioDonaciones.save(donacion);

        return perfilActualizado;
    }

    /**
     * Aplica una donación al perfil y, si completó la misión, le pasa la siguiente.
     *
     * @return {@code true} si el donante completó la misión con esta donación. Es lo que
     *         viaja en la respuesta y lo que queda guardado en la fila para poder repetir la
     *         misma ante un reintento (punto 14).
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
     * Le pasa al donante la siguiente misión de su secuencia, que puede ser de la misma
     * categoría o de la siguiente.
     *
     * <p><b>Acá no se llama a ningún servicio externo</b> (punto 12). Antes sí: pedía el
     * contacto a {@code donaciones-service} para meterlo en el evento, y lo hacía con la
     * transacción abierta, reteniendo una conexión del pool durante la llamada. Ahora el evento
     * lleva el {@code idUsuario} y el listener resuelve el contacto en {@code AFTER_COMMIT},
     * que es exactamente para lo que existe esa fase.
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
     * Cambia el nombre de usuario del donante.
     *
     * <p>Solo el nombre: la categoría y la misión las mueve el avance, no el usuario. Un
     * nombre vacío se ignora en vez de dejar el perfil sin nombre.
     */
    @Transactional
    public PerfilDTO actualizarDatosPerfil(UUID idUsuario, PerfilDTO dto) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isEmpty()) {
            p.cambiarNombre(dto.getNombreUsuario());
        }

        Perfil actualizado = repositorioPerfiles.save(p);

        return convertirPerfilADTO(actualizado);
    }

    // ========== CONVERTIDORES ==========

    /**
     * Proyecta el perfil al DTO de respuesta. Centraliza los null checks porque la
     * misma construcción estaba copiada en crearPerfil, buscarPorIdUsuario y
     * actualizarDatosPerfil, y cada copia podía divergir.
     */
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

    /**
     * Traduce el DTO de la donación a la entidad.
     *
     * <p>El {@code idDonacion} va en el constructor y no en un setter aparte: es la
     * primary key que hace idempotente la ingesta (punto 14), así que conviene que sea
     * parte de construir la entidad y no algo que se pueda olvidar después.
     */
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
     * Arma el DTO de la misión vigente con el avance del donante. El enunciado pide
     * poder ver "el progreso de su misión actual y la distancia restante hacia el
     * objetivo", así que el progreso se expone en la respuesta.
     *
     * <p>El faltante se calcula sobre el {@code progresoObjetivo} de la operación. Para
     * {@code VALORES_DISTINTOS} la regla además exige alcanzar cierta cantidad de
     * valores diferentes, así que el faltante puede llegar a 0 sin que la misión esté
     * completa. Es una limitación conocida del modelo, no de este cálculo.
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
     * Borra el perfil del donante.
     *
     * <p>Devuelve {@code void} y no {@code Boolean}: antes devolvía siempre {@code true} o
     * lanzaba, así que el valor de retorno no le decía nada a nadie. El 404 va por
     * excepción ({@link InexistenteException}), que es lo que permite que el handler lo
     * traduzca.
     */
    @Transactional
    public void eliminarPerfil(UUID idUsuario) {
        if (!repositorioPerfiles.existsByIdUsuario(idUsuario)) {
            throw new InexistenteException();
        }
        repositorioPerfiles.deleteByIdUsuario(idUsuario);
    }
}
