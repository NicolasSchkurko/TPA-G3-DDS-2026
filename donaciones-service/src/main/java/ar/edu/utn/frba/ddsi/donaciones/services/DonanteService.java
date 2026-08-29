package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorPersonas;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonantes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class DonanteService {

  private final GestorDonantes gestorDonantes;
  private final GestorPersonas gestorPersonas;
  private final NotificacionesClient notificacionClient;
  private final FabricaEstrategiasNotificacion fabricaEstrategias;
  private final IncentivosClient incentivosClient;
  private final RepositorioDonantes repositorioDonantes;

  public DonanteService(GestorDonantes gestorDonantes, GestorPersonas gestorPersonas,
                        NotificacionesClient notificacionClient, FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient, RepositorioDonantes repositorioDonantes) {
    this.gestorDonantes = gestorDonantes;
    this.gestorPersonas = gestorPersonas;
    this.notificacionClient = notificacionClient;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient = incentivosClient;
    this.repositorioDonantes = repositorioDonantes;
  }

  public PersonaDonanteDTO crearPersona(PersonaDonanteDTO dto) {
    Donante nuevoDonante = dto.toDomain();

    if (nuevoDonante.getPersona() != null) {
      String nombreUsuario = nuevoDonante.getPersona().getNombreDeUsuario();
      incentivosClient.peticionCrearPerfil(new IDDTO(nuevoDonante.getId(), nombreUsuario));

      if (nuevoDonante.getPersona().getMediosDeContacto() != null && nuevoDonante.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado() != null) {
        MedioDeContacto predeterminado = nuevoDonante.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado();
        NotificacionDTO notificacion = new NotificacionDTO(
            predeterminado.getTipo().toUpperCase(), predeterminado.getValor(),
            "Gracias por registrarse en DonaTrack", "Nuevo Registro en DonaTrack"
        );
        notificacionClient.enviarNotificacion(notificacion);
      }

      gestorPersonas.registrarPersona(nuevoDonante.getPersona());
    }
    gestorDonantes.registrarDonante(nuevoDonante);
    return PersonaDonanteDTO.from(nuevoDonante);
  }

  public List<PersonaDonanteDTO> listarTodas() {
    return repositorioDonantes.obtenerTodos().stream()
                         .map(PersonaDonanteDTO::from)
                         .collect(Collectors.toList());
  }

  public PersonaDonanteDTO buscarPorId(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    return PersonaDonanteDTO.from(donante);
  }

  public PersonaDonanteDTO buscarPorNombre(String nombre) {
    return repositorioDonantes.obtenerTodos().stream()
                         .filter(d -> d.getPersona().getNombreDeUsuario().equalsIgnoreCase(nombre))
                         .findFirst()
                         .map(PersonaDonanteDTO::from).orElse(null);
  }

  public PersonaDonanteDTO actualizarPersona(UUID id, PersonaDonanteDTO dto) {
    Donante datosNuevos = dto.toDomain();
    Donante existente = repositorioDonantes.buscarPorId(id).orElse(null);
    if (existente == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    if (existente.getPersona() != null && datosNuevos.getPersona() != null) {
      gestorPersonas.modificarPersona(existente.getPersona().getId(), datosNuevos.getPersona());
    }
    return PersonaDonanteDTO.from(gestorDonantes.modificarDonante(id, datosNuevos));
  }

  public void eliminarPersona(UUID id) {
    repositorioDonantes.eliminarPorId(id);
    System.out.println("Donante dado de baja (si existía).");
  }

  public String importarDonantes(MultipartFile file, List<MapeoCSV> mapeosCsv) {
    try {
      byte[] bytes = file.getBytes();
      CompletableFuture.runAsync(() -> {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
          LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosCsv));
          List<Donante> importados = lector.importar(is);
          for (Donante d : importados) {
            try { crearPersona(PersonaDonanteDTO.from(d)); }
            catch (Exception ignored) {}
          }
        } catch (IOException ignored) {}
      });
      return "Importación en segundo plano iniciada.";
    } catch (IOException e) { throw new RuntimeException("Error CSV", e); }
  }

  public List<MediosContactoDTO> obtenerMediosContacto(UUID id) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    if (donante.getPersona().getMediosDeContacto() != null && donante.getPersona().getMediosDeContacto().getListaMediosDeContacto() != null) {
      return donante.getPersona().getMediosDeContacto().getListaMediosDeContacto().stream().map(MediosContactoDTO::from).collect(Collectors.toList());
    }
    return List.of();
  }

  public PersonaDonanteDTO agregarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    gestorPersonas.agregarMedioDeContactoAPersona(donante.getPersona().getId(), dto.toDomain());
    return PersonaDonanteDTO.from(donante);
  }

  public PersonaDonanteDTO eliminarMedioContacto(UUID id, MediosContactoDTO dto) {
    Donante donante = repositorioDonantes.buscarPorId(id).orElse(null);
    if (donante == null) throw new IllegalArgumentException("No se encontró persona con ID: " + id);
    gestorPersonas.eliminarMedioDeContactoAPersona(donante.getPersona().getId(), dto.toDomain());
    return PersonaDonanteDTO.from(donante);
  }

  public void revisarActividades(){
    for (Donante p : repositorioDonantes.obtenerTodos()) {
      if (p.getFormularios() != null && !p.getFormularios().isEmpty() && p.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())) {
        fabricaEstrategias.obtenerEstrategia(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE).ejecutar(p);
      }
    }
  }
}