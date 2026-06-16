package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilDuplicadoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Mensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PersonaService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();
    private final PerfilService perfilService;
    private final NotificacionClient notificacionClient;
    private final DonacionClient donacionClient;


    public PersonaService(PerfilService metricasService,
                          NotificacionClient notificacionClient,
                          DonacionClient donacionClient) {
        this.perfilService = metricasService;
        this.notificacionClient = notificacionClient;
        this.donacionClient = donacionClient;
    }

    public void crearPerfil(PerfilDonanteDTO dto) {
            if (dto.getIdUsuario() == null) {
                throw new DatosInvalidosException();
            }

            if (repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario()) != null) {
                throw new PerfilDuplicadoException();
            }

            Perfil nuevo = new Perfil(dto.getIdUsuario(), dto.getNombreUsuario());

            // Para que el perfil nuevo no nazca con la misión en null, se la seteamos acá usando el repo
            var categoriaBase = ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias.getInstance()
                    .buscarPorTipo(nuevo.getCategoriaActual());
            if (categoriaBase != null) {
                nuevo.setMisionActual(categoriaBase.primeraMision());
            }

            repositorioPerfiles.agregarPerfil(nuevo);
        }

    public void actualizarPerfil(ImpactoDonacionDTO dto) {
        if (dto.getIdUsuario() == null) {
            throw new DatosInvalidosException();
        }

        if (repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario()) == null) {
            throw new PerfilInexistenteException();
        }

        ImpactoDonacion donacion = this.convertirDTO(dto);
        Perfil perfil = repositorioPerfiles.buscarPorIDUsuario(dto.getIdUsuario());

        Perfil perfilAnterior = perfil.clonar();
        perfilService.progresarMisionesPerfil(perfil, donacion);

        //si hay algun cambio actualizo repo (cambie la verificacion proque si tenia algun argumento en null iba a tirar un NullPointerException)
        boolean cambioMision = (perfilAnterior.getMisionActual() == null && perfil.getMisionActual() != null) ||
                (perfilAnterior.getMisionActual() != null && !perfilAnterior.getMisionActual().equals(perfil.getMisionActual()));

        if (cambioMision) {
            repositorioPerfiles.actualizar(perfil);
        }

        if(!perfilAnterior.getCategoriaActual().equals(perfil.getCategoriaActual())){
            repositorioPerfiles.actualizar(perfil);
            //enviar notificacion
            MedioContactoDTO contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
            Mensaje mensaje = this.crearMensajeCategoria(perfil.getNombreUsuario(),
                    perfilAnterior.getCategoriaActual().name(),
                    perfil.getCategoriaActual().name(),
                    "Ascenso Categoria"
            );
            PerfilNotificacionDTO notificacion = new PerfilNotificacionDTO(
                    contacto.getMedioDeContacto(),
                    contacto.getDireccionContacto(),
                    mensaje.getCuerpoMensaje(),
                    mensaje.getAsuntoMensaje()
                    );

            notificacionClient.enviarNotificacion(notificacion);
        }
        if(!perfilAnterior.getInsignias().equals(perfil.getInsignias())){
            repositorioPerfiles.actualizar(perfil);
            //enviar notificacion
            MedioContactoDTO contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());
            Mensaje mensaje = this.crearMensajeInsignia(perfil.getNombreUsuario(),
                    perfil.getInsignias().getLast(),
                    "Insignia Nueva"
            );
            PerfilNotificacionDTO notificacion = new PerfilNotificacionDTO(
                    contacto.getMedioDeContacto(),
                    contacto.getDireccionContacto(),
                    mensaje.getCuerpoMensaje(),
                    mensaje.getAsuntoMensaje()
            );

            notificacionClient.enviarNotificacion(notificacion);
        }
    }

    public Mensaje crearMensajeCategoria(String nombreUsuario,
                                         String categoriaAnterior,
                                         String categoriaActual,
                                         String asunto) {
        return new Mensaje("Felicitaciones "
                +nombreUsuario
                +", has ascendido de "
                +categoriaAnterior
                + " a la nueva categoria "
                +categoriaActual, asunto);
    }

    public Mensaje crearMensajeInsignia(String nombreUsuario,
                                         Insignia insignia,
                                         String asunto) {
        return new Mensaje("Felicitaciones "
                +nombreUsuario
                +", has conseguido una nueva Insignia: "
                + insignia.getNombre() + "/n"
                + insignia.getDescripcion() + "/n"
                + insignia.getUrlImagen(), asunto);
    }

    public ImpactoDonacion convertirDTO(ImpactoDonacionDTO donacion){
        return new ImpactoDonacion(donacion.getEntidadBeneficiaria(),
                donacion.getCantidadBienes(),
                donacion.getFechaEntrega(),
                donacion.getCategoria(),
                donacion.getSubCategoria(),
                donacion.getEstado(),
                donacion.getIdUsuario());
    }
}