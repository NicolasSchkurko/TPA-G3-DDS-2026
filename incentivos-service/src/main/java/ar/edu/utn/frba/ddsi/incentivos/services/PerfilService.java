package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;
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
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.GestorPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {
    private final RepositorioPerfiles repositorioPerfiles;
    private final GestorPerfiles perfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioDonaciones repositorioDonaciones;

    public PerfilService(RepositorioPerfiles repositorioPerfiles,
                         GestorPerfiles perfiles,
                         RepositorioCategorias repositorioCategorias,
                         RepositorioDonaciones repositorioDonaciones) {
        this.repositorioPerfiles = repositorioPerfiles;
        this.perfiles = perfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.repositorioDonaciones = repositorioDonaciones;
    }

    //para misionesScheduler
    public void evaluarConstanciaPerfiles(){
        List<Perfil> perfilesConMision = repositorioPerfiles.findAll().stream()
                .filter(perfil -> perfil.getProgresoMisionActual() != null)
                .filter(perfil -> perfil.getProgresoMisionActual().getMision() != null)
                .filter(perfil -> perfil.getProgresoMisionActual().getMision()
                        .getReglaDeProgreso().getConstancia() != null)
                .toList();

        perfilesConMision = perfiles.evaluarProgresosConstantes(perfilesConMision);

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
    public PerfilDTO buscarPorIdUsuario(UUID idUsuario) {
        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario).orElse(null);
        if (p == null) {
            return null;
        }

        return new PerfilDTO(
            p.getNombreUsuario(),
            p.getCategoriaActual() == null ? null : p.getCategoriaActual().getNombre(),
            p.getInsigniasObtenidas() == null ? List.of() : p.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList(),
            p.getProgresoMisionActual() == null ? null : p.getProgresoMisionActual().getMision().getNombreMision()
        );
    }

    public List<InsigniaDTO> obtenerInsigniasPorIdUsuario(UUID idUsuario) {
        repositorioPerfiles.findByIdUsuario(idUsuario)
                           .orElseThrow(InexistenteException::new);

        return repositorioPerfiles.obtenerInsigniasPorIdUsuario(idUsuario)
                                  .stream()
                                  .map(this::convertirInsigniaADTO)
                                  .toList();
    }

    public MisionPerfilDTO obtenerMisionPorIdUsuario(UUID idUsuario) {
        repositorioPerfiles.findByIdUsuario(idUsuario)
                           .orElseThrow(InexistenteException::new);

        Mision mision = repositorioPerfiles.obtenerMisionPorIdUsuario(idUsuario)
                                           .orElseThrow();
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

        Boolean perfilActualizado = perfiles.progresarPerfil(p, donacion);

        repositorioPerfiles.save(p);
        repositorioDonaciones.save(donacion);

        return perfilActualizado;
        //false = no consiguio insignia -pudo o no haber progresado-
        //true = consiguio insignia
    }

        // ========== ACTUALIZAR ==========
    @Transactional
    public PerfilDTO actualizarDatosPerfil(UUID idUsuario, PerfilDTO dto) {
        if (idUsuario == null) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo");
        }

        Perfil p = repositorioPerfiles.findByIdUsuario(idUsuario)
                                      .orElseThrow(InexistenteException::new);

        // Actualizar nombre de usuario
        // -sapo: es lo unico que se puede editar en perfil x ahora
        if (dto.getNombreUsuario() != null && !dto.getNombreUsuario().isEmpty()) {
            p.setNombreUsuario(dto.getNombreUsuario());
        }

        // Nota: categoriaActual, insignias y misionActual se actualizan mediante otros métodos
        // del dominio (progresión de misiones, cambios de categoría, etc.)
        // No se actualizan directamente desde aquí por consistencia del modelo de dominio

        Perfil actualizado = repositorioPerfiles.save(p);

        return new PerfilDTO(
            actualizado.getNombreUsuario(),
            actualizado.getCategoriaActual().getNombre(),
            actualizado.getInsigniasObtenidas() == null ? List.of() : actualizado.getInsigniasObtenidas().stream().map(io -> io.getInsignia().getNombre()).toList(),
            actualizado.getProgresoMisionActual().getMision().getNombreMision()
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

/*
⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⣿⠟⠄⠹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⡟⠄⠄⠄⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠇⠄⠄⠄⠄⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⢿⣿⠿⠿⠿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠃⡃⡛⠈⣹⣋⣲⡐⠂⢸⡇⡘⢣⠄⣠⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⠃⠄⠄⠄⠄⠄⠘⣿⣿⣿⣿⣿⣿⠿⠈⠹⢿⣠⠄⢸⣿⠄⠶⠶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⡆⠰⠸⠤⢿⣡⣓⡒⡂⣒⡇⢃⢶⠄⣶⣾⣿⣿⣿
⣿⣿⣿⣿⣿⣿⠏⠄⠄⠄⠄⠄⠄⠄⠙⣿⣿⣿⣿⣿⣶⡀⣰⣾⣿⠄⢸⣟⠛⠿⠄⣽⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣿⣶⣿⣾⣿⣿⣿⣶⣿⣷⣿⣿⣶⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⠄⠄⠄⠄⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⣾⣿⣷⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣃⣀⣀⠄⠄⠄⠄⠄⣀⣀⣈⣿⡿⠿⢿⣿⣿⣿⣿⣿⣿⣿⣿⠿⢿⣿⣿⣿⡿⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⣇⠄⠷⣤⡼⠋⡉⠻⡿⢉⡉⢻⠉⢹⠋⣉⠛⣿⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⢫⣍⣴⣴⢴⣾⣴⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⡏⠙⠷⠄⠄⠘⠟⠄⣇⠸⠟⢻⠄⢸⠄⠶⠄⣇⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠿⠟⠚⠛⠿⢿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡏⠉⣿⣿⣿⠃⠐⠒⠠⠄⠄⠠⠄⠄⠄⢻⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⣿⡿⠛⠿⢿⣿⣿⣿⣿⣿⣿⣿⣿⡟⢻⠟⣿⠿⢿⣿⣿⣿⣿⠏⠙⠃⠄⢻⣿⡇⠄⠄⡔⠄⠄⠄⠄⠠⡀⠄⠄⢿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⡏⠄⣶⣦⣼⠉⢉⡝⠉⡉⠙⡏⢉⡁⢸⠁⣏⠄⢸⣿⣿⣿⣿⡆⠄⠄⠄⢸⣿⡆⠄⠘⠄⠄⠄⠄⡀⠄⢱⠄⠄⣾⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⣧⡀⠛⠋⢹⠄⢸⣇⠄⠖⠲⡇⠻⠃⢸⠄⣿⠄⢸⣿⣿⣿⣿⣧⡀⢀⣤⣾⣿⣷⡄⠸⡀⠄⢡⠰⠄⠄⡎⠄⣴⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⣿⣿⣶⣾⣿⣶⣾⣿⣷⣶⣿⣿⣶⣶⣾⣶⣿⣷⣾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⣵⣀⢸⢀⢀⣨⣴⣾⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⠄⠄⠄⠄⠄⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿
* */

