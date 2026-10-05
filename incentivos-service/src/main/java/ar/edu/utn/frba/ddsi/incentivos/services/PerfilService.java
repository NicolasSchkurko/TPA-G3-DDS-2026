package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.InsigniaDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.CategoriaBaseInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilExistenteException;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

        return new PerfilDTO(
            nuevo.getNombreUsuario(),
            nuevo.getCategoriaActual() != null ? nuevo.getCategoriaActual().getNombre() : null,
            nuevo.getInsigniasObtenidas() != null ? nuevo.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList() : List.of(),
            nuevo.getProgresoMisionActual() != null ? nuevo.getProgresoMisionActual().getMision().getNombreMision() : null
        );
    }

    // ========== BUSCAR ==========
    @Transactional(readOnly = true)
    public PerfilDTO buscarPorIdUsuario(UUID idUsuario) {
        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario).orElse(null);
        if (p == null) {
            return null;
        }

        return new PerfilDTO(
            p.getNombreUsuario(),
            p.getCategoriaActual() == null ? null : p.getCategoriaActual().getNombre(),
            p.getInsigniasObtenidas() == null ? List.of() : p.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList(),
            p.getProgresoMisionActual() == null || p.getProgresoMisionActual().getMision() == null
               ? null
               : p.getProgresoMisionActual().getMision().getNombreMision()
        );
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
        Mision mision = repositorioPerfiles.obtenerMisionPorIdUsuario(idUsuario)
                                           .orElseThrow(InexistenteException::new);
        return convertirMisionPerfilADTO(mision);
    }

    // ========== ACTUALIZAR ==========
    @Transactional
    public Boolean actualizarPerfilImpacto(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            return null;
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        Boolean perfilActualizado = this.progresarPerfil(p, donacion);

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

        return new PerfilDTO(
            actualizado.getNombreUsuario(),
            actualizado.getCategoriaActual() == null ? null : actualizado.getCategoriaActual().getNombre(),
            actualizado.getInsigniasObtenidas() == null ? List.of() : actualizado.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList(),
            actualizado.getProgresoMisionActual() == null || actualizado.getProgresoMisionActual().getMision() == null
               ? null
               : actualizado.getProgresoMisionActual().getMision().getNombreMision()
        );
    }

    // ========== CONVERTIDORES ==========
    public ImpactoDonacion convertirDTO(UUID id, ImpactoDonacionDTO donacion) {
        return new ImpactoDonacion(
            donacion.getEntidadBeneficiaria(),
            donacion.getCantidadBienes(),
            donacion.getFechaEntrega(),
            donacion.getCategoria(),
            donacion.getSubCategoria(),
            donacion.getEstado(),
            id);
    }

    public MisionPerfilDTO convertirMisionPerfilADTO(Mision mision) {
        return new MisionPerfilDTO(
            mision.getNombreMision(),
            mision.getDescripcion(),
            mision.getInsigniaObjetivo().getNombre()
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
