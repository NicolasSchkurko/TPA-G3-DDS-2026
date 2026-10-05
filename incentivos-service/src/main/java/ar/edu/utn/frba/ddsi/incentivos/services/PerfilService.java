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
import org.springframework.transaction.annotation.Transactional;

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
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioDonaciones repositorioDonaciones;

    public PerfilService(RepositorioPerfiles repositorioPerfiles,
                         RepositorioCategorias repositorioCategorias,
                         RepositorioDonaciones repositorioDonaciones) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioDonaciones = repositorioDonaciones;
    }

    /**
     * Recalcula la racha de todos los que están en una misión con constancia.
     *
     * <p>Lo llama el scheduler: la racha caduca por el paso del tiempo, no por una
     * donación, así que sin esta pasada un donante que dejó de donar queda con el avance
     * congelado y la misión nunca aparece como pendiente.
     */
    @Transactional
    public void evaluarConstanciaPerfiles() {
        List<Perfil> perfilesConMision = repositorioPerfiles.buscarPerfilesConMisionQueRequiereConstancia();

        perfilesConMision.forEach(perfil -> perfil.verificarProgresoMision(
            repositorioDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
                perfil.getIdUsuario(),
                perfil.getProgresoMisionActual().getMision().getIdMision())
        ));

        repositorioPerfiles.saveAll(perfilesConMision);
    }

    /**
     * Crea el perfil de un donante y lo deja listo para empezar: categoría base y primera
     * misión.
     *
     * <p>Se verifica que el donante no tenga perfil antes de armar nada, así el error de
     * duplicado no deja un agregado a medio construir.
     */
    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        // Se chequea antes de armar nada: si el donante ya existe, no tiene sentido
        // resolver la categoría base ni tocar la base de datos.
        if (repositorioPerfiles.existsByIdUsuario(dto.getIdUsuario())) {
            throw new PerfilExistenteException(dto.getIdUsuario());
        }

        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

        Categoria categoriaBase = repositorioCategorias.findAllByOrderByPosicionSecuenciaAsc().stream()
                                                       .findFirst()
                                                       .orElseThrow(() -> new CategoriaBaseInexistenteException(
                                                           "No existe la categoría base configurada"));

        // El agregado arma su propio estado inicial: categoría y primera misión.
        nuevo.iniciarEn(categoriaBase);

        return convertirPerfilADTO(repositorioPerfiles.save(nuevo));
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
    @Transactional
    public boolean actualizarPerfilImpacto(UUID idUsuario, ImpactoDonacionDTO dto) {
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
