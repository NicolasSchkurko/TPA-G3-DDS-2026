package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.*;

import ar.edu.utn.frba.ddsi.incentivos.exceptions.DatosInvalidosException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilDuplicadoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.PerfilInexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class PersonaService {
    private final RepositorioDonaciones repositorioDonaciones;
    private final RepositorioPerfiles repositorioPerfiles;
    private final RepositorioCategorias repositorioCategorias;
    private final NotificacionClient notificacionClient;
    private final DonacionClient donacionClient;
    private final N8nClient n8nClient;


    public PersonaService(NotificacionClient notificacionClient,
                          DonacionClient donacionClient,
                          N8nClient n8nClient,
                          RepositorioDonaciones repositorio,
                          RepositorioPerfiles perfiles,
                          RepositorioCategorias repositorioCategorias) {
        this.notificacionClient = notificacionClient;
        this.donacionClient = donacionClient;
        this.n8nClient = n8nClient;
        this.repositorioDonaciones = repositorio;
        this.repositorioPerfiles = perfiles;
        this.repositorioCategorias = repositorioCategorias;
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
            //el progreso en una mision debe actualizar el repo de donaciones
            repositorioDonaciones.actualizar(perfil);
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

            notificacionClient.enviarNotificacion(notificacion);
        }
        if(!perfilAnterior.getInsignias().equals(perfil.getInsignias())){
            //enviar notificacion
            MedioContactoDTO contacto = donacionClient.obtenerContactoPersona(perfil.getIdUsuario());

            n8nClient.publicarInsignia(
                    perfil.getNombreUsuario(),
                    perfil.getInsignias().getLast().getNombre(),
                    "formato circulo, diseño estrella, color dorado, debe incluir el texto "
                            + perfil.getInsignias().getLast().getNombre() + " centrado "
            );

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