package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.InsigniaObtenida;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioDonaciones repositorioDonaciones;
    private final DonacionClient donacionClient;

    public PerfilService(RepositorioPerfiles repositorioPerfiles,
                         RepositorioCategorias repositorioCategorias,
                         RepositorioDonaciones repositorioDonaciones,
                         DonacionClient donacionClient) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioDonaciones = repositorioDonaciones;
        this.donacionClient = donacionClient;
    }

    //para misionesScheduler
    @Transactional
    public void evaluarConstanciaPerfiles(){
        List<Perfil> perfilesConMision = repositorioPerfiles.buscarPerfilesConMisionQueRequiereConstancia();

        perfilesConMision.forEach(perfil -> perfil.verificarProgresoMision(
            repositorioDonaciones.findByIdUsuarioAndIdMisionOrderByFechaEntregaAsc(
                perfil.getIdUsuario(),
                perfil.getProgresoMisionActual().getMision().getIdMision())
        ));

        repositorioPerfiles.saveAll(perfilesConMision);
    }

    // ========== CREAR ==========
    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

        Categoria categoriaBase = repositorioCategorias.findAllByOrderByPosicionSecuenciaAsc().stream()
                                                       .findFirst()
                                                       .orElseThrow(() -> new CategoriaBaseInexistenteException(
                                                           "No existe la categoría base configurada"));

        nuevo.setCategoriaActual(categoriaBase);

        if (categoriaBase.primeraMision() != null) {
            nuevo.setProgresoMisionActual(new ProgresoMision(categoriaBase.primeraMision()));
        }

        if (repositorioPerfiles.existsByIdUsuario(nuevo.getIdUsuario())) {
            throw new PerfilExistenteException(nuevo.getIdUsuario());
        }

        nuevo = repositorioPerfiles.save(nuevo);

        return convertirPerfilADTO(nuevo);
    }

    // ========== BUSCAR ==========
    @Transactional(readOnly = true)
    public PerfilDTO buscarPorIdUsuario(UUID idUsuario) {
        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(() -> new InexistenteException(
                                          "No existe un perfil para el usuario " + idUsuario
                                      ));

        return convertirPerfilADTO(p);
    }

    @Transactional(readOnly = true)
    public Page<InsigniaDTO> obtenerInsigniasPorIdUsuario(UUID idUsuario, Pageable pageable) {
        if (!repositorioPerfiles.existsByIdUsuario(idUsuario)) {
            throw new InexistenteException();
        }

        return repositorioPerfiles.paginaInsigniasPorIdUsuario(idUsuario, pageable)
                                  .map(obtenida -> convertirInsigniaADTO(obtenida.getInsignia()));
    }

    @Transactional(readOnly = true)
    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        ProgresoMision progreso = repositorioPerfiles.obtenerProgresoMisionPorIdUsuario(idUsuario)
                                                .orElseThrow(InexistenteException::new);
        return convertirProgresoMisionADTO(progreso);
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
    public Boolean actualizarPerfilImpacto(UUID idUsuario, ImpactoDonacionDTO dto) {
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

        Boolean perfilActualizado = this.progresarPerfil(p, donacion);
        // Se guarda para poder repetir la misma respuesta ante un reintento.
        donacion.setCompletMision(perfilActualizado);

        repositorioPerfiles.save(p);
        repositorioDonaciones.save(donacion);

        return perfilActualizado;
    }

    private Boolean progresarPerfil(Perfil perfil, ImpactoDonacion donacion) {
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

        Boolean misionCompletada = perfil.progresarMision(donacion, donaciones);
        if (!misionCompletada || misionActual == null) {
            return misionCompletada;
        }

        this.asignarSiguienteMision(perfil, misionActual);
        return true;
    }

    private void asignarSiguienteMision(Perfil perfil, Mision misionCompletada) {
        Categoria categoriaActual = perfil.getCategoriaActual();
        if (categoriaActual == null) {
            perfil.setProgresoMisionActual(null);
            return;
        }

        Mision siguienteMision = categoriaActual.siguienteMision(misionCompletada);
        if (siguienteMision != null) {
            MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
            perfil.cambiarMision(siguienteMision, misionCompletada, contacto);
            return;
        }

        Categoria siguienteCategoria = repositorioCategorias
            .obtenerCategoriaSiguiente(categoriaActual);

        if (siguienteCategoria != null) {
            MedioContacto contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
            perfil.cambiarCategoria(
                siguienteCategoria,
                categoriaActual,
                misionCompletada,
                contacto
            );
            return;
        }

        perfil.setProgresoMisionActual(null);
    }

    @Transactional
    public PerfilDTO actualizarDatosPerfil(UUID idUsuario, PerfilDTO dto) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isEmpty()) {
            p.setNombreUsuario(dto.getNombreUsuario());
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
        List<InsigniaObtenida> insignias = perfil.getInsigniasObtenidas();
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

    public ImpactoDonacion convertirDTO(UUID id, ImpactoDonacionDTO donacion) {
        ImpactoDonacion entidad = new ImpactoDonacion(
            donacion.getEntidadBeneficiaria(),
            donacion.getCantidadBienes(),
            donacion.getFechaEntrega(),
            donacion.getCategoria(),
            donacion.getSubCategoria(),
            donacion.getEstado(),
            id);
        entidad.setIdDonacion(donacion.getIdDonacion());
        return entidad;
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

    public InsigniaDTO convertirInsigniaADTO(Insignia insignia) {
        return new InsigniaDTO(
            insignia.getNombre(),
            insignia.getDescripcion(),
            insignia.getUrlImagen()
        );
    }

    // ========== ELIMINAR ==========
    @Transactional
    public Boolean eliminarPerfil(UUID idUsuario) {
        if (!repositorioPerfiles.existsByIdUsuario(idUsuario)) {
            throw new InexistenteException();
        }
        repositorioPerfiles.deleteByIdUsuario(idUsuario);
        return true;
    }
}
