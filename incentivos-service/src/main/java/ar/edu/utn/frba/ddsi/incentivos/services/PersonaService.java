package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilDuplicadoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class PersonaService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();
    private final RepositorioCategorias repositorioCategorias = RepositorioCategorias.getInstance();
    private final RankingService rankingService;
    private final PerfilService perfilService;
    private final NotificacionClient notificacionClient;
    private final DonacionClient donacionClient;


    public PersonaService(PerfilService metricasService,
                          NotificacionClient notificacionClient,
                          DonacionClient donacionClient,
                          RankingService rankingService) {
        this.perfilService = metricasService;
        this.notificacionClient = notificacionClient;
        this.donacionClient = donacionClient;
        this.rankingService = rankingService;
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
            Categoria categoriaBase = repositorioCategorias.buscarPorTipo(nuevo.getCategoriaActual());
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

            notificacionClient.enviarNotificacion(notificacion);
        }
        if(!perfilAnterior.getInsignias().equals(perfil.getInsignias())){
            repositorioPerfiles.actualizar(perfil);
            perfil.sumarMisionCumplida();
            //enviar notificacion
            MedioContactoDTO contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());

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

            notificacionClient.enviarNotificacion(notificacion);
        }
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