package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.clients.N8nClient;
import ar.edu.utn.frba.ddsi.incentivos.clients.NotificacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.*;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PersonaService {
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final RepositorioNotificacionesPendientes pendientes; //en un new service decido que hacer con estas excepciones
    private final RepositorioPublicacionesPendientes publicacionesPendientes; //en un new service decido que hacer con estas excepciones
    private final NotificacionClient notificacionClient;
    private final DonacionClient donacionClient;
    private final N8nClient n8nClient;


    public PersonaService(NotificacionClient notificacionClient,
                          DonacionClient donacionClient,
                          N8nClient n8nClient,
                          RepositorioDonaciones repositorio,
                          RepositorioPerfiles perfiles,
                          RepositorioCategorias repositorioCategorias,
                          RepositorioNotificacionesPendientes pendientes,
                          RepositorioPublicacionesPendientes publicacionesPendientes) {
        this.notificacionClient = notificacionClient;
        this.donacionClient = donacionClient;
        this.n8nClient = n8nClient;
        this.repositorioDonaciones = repositorio;
        this.repositorioPerfiles = perfiles;
        this.repositorioCategorias = repositorioCategorias;
        this.pendientes = pendientes;
        this.publicacionesPendientes = publicacionesPendientes;
    }

    public PerfilDTO crearPerfil(PerfilDonanteDTO dto) {
        if (dto.getIdUsuario() == null) {
            throw new DatosInvalidosException();
        }

        if (repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario()) != null) {
            throw new PerfilDuplicadoException();
        }

        Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

        // Para que el perfil nuevo no nazca con la misión en null, se la seteamos acá usando el repo
        Categoria categoriaBase = repositorioCategorias.buscarPorTipo(nuevo.getCategoriaActual());
        if (categoriaBase != null) {
            nuevo.setMisionActual(categoriaBase.primeraMision());
        }
        repositorioPerfiles.agregarPerfil(nuevo);

        PerfilDTO pDTO = new PerfilDTO(
                nuevo.getNombreUsuario(),
                nuevo.getCategoriaActual().name(),
                nuevo.getInsignias().stream().map(Insignia::getNombre).toList(),
                nuevo.getMisionActual().getNombreMision(),
                nuevo.getPosicionRanking().getPuesto()
        );

        return pDTO;
    }

    public PerfilDTO actualizarPerfil(UUID idUsuario, ImpactoDonacionDTO dto) {
        if (idUsuario == null) {
            throw new DatosInvalidosException();
        }

        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(idUsuario);
        if (perfil == null) {
            throw new PerfilInexistenteException();
        }

        ImpactoDonacion donacion = this.convertirDTO(idUsuario, dto);
        Perfil perfilAnterior = perfil.clonar();

        perfil.progresarMision(donacion);

        //evalua si hubo un cambio en el progreso de la mision
        boolean cambioMision = (perfilAnterior.getMisionActual() == null
                                && perfil.getMisionActual() != null) ||
                        (perfilAnterior.getMisionActual() != null
                        && !perfilAnterior.getMisionActual().equals(perfil.getMisionActual()));

        if (cambioMision) {
            //evalua si el progreso en mision requiere pasar a siguiente mision o categoria
            // en base a eso actualiza el perfil en el repo de perfiles
            Categoria categoriaObj = repositorioCategorias.buscarPorTipo(perfil.getCategoriaActual());

            if (categoriaObj != null) {
                // Caso A: Era la última misión de la categoría, sube de nivel
                if (categoriaObj.esUltimaMision(perfil.getMisionActual())) {
                    TipoCategoria siguienteNivel = categoriaObj.getSiguienteCategoria();
                    perfil.setCategoriaActual(siguienteNivel);

                    // Le asignamos la primera misión del nuevo rango
                    Categoria nuevaCategoria = repositorioCategorias.buscarPorTipo(siguienteNivel);
                    perfil.setMisionActual(nuevaCategoria != null ? nuevaCategoria.primeraMision() : null);
                }
                // Caso B: Quedan misiones en esta categoría, avanzamos a la siguiente
                else {
                    perfil.setMisionActual(categoriaObj.siguienteMision(perfil.getMisionActual()));
                }
            }

            repositorioPerfiles.actualizar(perfil);
            //el progreso en una mision debe guardar la donacion en el repo donaciones
            repositorioDonaciones.guardar(donacion);
        }

        if(!perfilAnterior.getCategoriaActual().equals(perfil.getCategoriaActual())){
            //enviar notificacion
            MedioContactoDTO contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());

            PerfilNotificacionDTO notificacion = new PerfilNotificacionDTO(
                    contacto.getMedioDeContacto(),
                    contacto.getDireccionContacto(),
                    "Felicitaciones "
                            +perfil.getNombreUsuario()
                            +", has ascendido de "
                            +perfilAnterior.getCategoriaActual().name()
                            + " a la nueva categoria "
                            +perfil.getCategoriaActual().name(),
                    "Ascenso Categoria"
                    );
            try {
                notificacionClient.enviarNotificacion(notificacion);
            }
            catch (EnvioNotificacionException e) {
                pendientes.guardar(e.getMensaje());
            }
        }
        if(!perfilAnterior.getInsignias().equals(perfil.getInsignias())){
            //enviar notificacion
            MedioContactoDTO contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());

            PerfilPublicacionDTO publicacion = new PerfilPublicacionDTO(perfil.getNombreUsuario(),
                    perfil.getInsignias().getLast().getNombre(),
                    "formato circulo, diseño estrella, color dorado, debe incluir el texto "
                            + perfil.getInsignias().getLast().getNombre() + " centrado ");

            try {
                n8nClient.publicarInsignia(publicacion);
            }
            catch (EnvioPublicacionException e) {
                publicacionesPendientes.guardar(e.getPublicacion());
            }

            PerfilNotificacionDTO notificacion = new PerfilNotificacionDTO(
                    contacto.getMedioDeContacto(),
                    contacto.getDireccionContacto(),
                    "Felicitaciones "
                            +perfil.getNombreUsuario()
                            +", has conseguido una nueva Insignia: "
                            + perfil.getInsignias().getLast().getNombre() + "/n"
                            + perfil.getInsignias().getLast().getDescripcion() + "/n"
                            + perfil.getInsignias().getLast().getUrlImagen(),
                    "Mision Completa"
            );

            try {
                notificacionClient.enviarNotificacion(notificacion);
            }
            catch (EnvioNotificacionException e) {
                pendientes.guardar(e.getMensaje());
            }
        }

        PerfilDTO pDTO = new PerfilDTO(
                perfil.getNombreUsuario(),
                perfil.getCategoriaActual().name(),
                perfil.getInsignias().stream().map(Insignia::getNombre).toList(),
                perfil.getMisionActual().getNombreMision(),
                perfil.getPosicionRanking().getPuesto()
        );

        return pDTO;
    }

    public ImpactoDonacion convertirDTO(UUID id, ImpactoDonacionDTO donacion){
        return new ImpactoDonacion(donacion.getEntidadBeneficiaria(),
                donacion.getCantidadBienes(),
                donacion.getFechaEntrega(),
                donacion.getCategoria(),
                donacion.getSubCategoria(),
                donacion.getEstado(),
                id);
    }
}