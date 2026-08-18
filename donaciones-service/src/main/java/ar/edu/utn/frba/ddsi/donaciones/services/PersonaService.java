package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PersonaService {

  // Inyección de dependencias en lugar de llamar al getInstance()
  private final GestorDonantes gestorDonantes;
  private final NotificacionesClient notificacionClient;
  private final FabricaEstrategiasNotificacion  fabricaEstrategias;
  private final IncentivosClient incentivosClient;

  public PersonaService(GestorDonantes gestorDonantes,
                        NotificacionesClient notificacionClient,
                        FabricaEstrategiasNotificacion fabricaEstrategias,
                        IncentivosClient incentivosClient) {
    this.gestorDonantes = gestorDonantes;
    this.notificacionClient = notificacionClient;
    this.fabricaEstrategias = fabricaEstrategias;
    this.incentivosClient=incentivosClient;
  }

  // --- CRUD METHODS ---

  public Donante crearPersona(Donante nuevaPersona) {

    // Obtenemos el nombre de usuario directo desde la Persona del donante
    String nombreUsuario = (nuevaPersona instanceof Humana) ?
                           ((Humana) nuevaPersona).getPersona().getNombreDeUsuario() :
                           ((Juridica) nuevaPersona).getNombreDeUsuario();

    IDDTO peticion = new IDDTO(
        nuevaPersona.getId(),
        nombreUsuario
    );
    incentivosClient.peticionCrearPerfil(peticion);

    if (nuevaPersona instanceof Juridica) {
      NotificacionDTO notificacionCreacionUsuario = new NotificacionDTO(
          nuevaPersona.getMediosDeContacto().getMedioDeContactoPredeterminado().getTipo().toLowerCase(),
          nuevaPersona.getMediosDeContacto().getMedioDeContactoPredeterminado().getValor(),
          "Gracias por registrarse en DonaTrack",
          "Nuevo Registro en DonaTrack"
      );
      notificacionClient.enviarNotificacion(notificacionCreacionUsuario);
    }

    gestorDonantes.registrarDonante(nuevaPersona);
    return nuevaPersona;
  }

  public List<Donante> listarTodas() {
    return gestorDonantes.listarTodosLosDonantes();
  }

  public Donante buscarPorId(UUID id) {
    Donante donante = gestorDonantes.obtenerDonante(id);
    if (donante == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }
    return donante;
  }

  public Donante buscarPorNombre(String nombreBuscado) {
    return gestorDonantes.listarTodosLosDonantes().stream()
                         .filter(d -> d.darNombre().equalsIgnoreCase(nombreBuscado))
                         .findFirst()
                         .orElse(null);
  }

  public Donante actualizarPersona(UUID id, Donante datosNuevos) {
    Donante existente = gestorDonantes.obtenerDonante(id);
    if (existente == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }

    if (existente instanceof Humana ph && datosNuevos instanceof Humana phNuevos) {
      ph.getPersona().setNombre(phNuevos.getPersona().getNombre());
      ph.getPersona().setApellido(phNuevos.getPersona().getApellido());
      ph.getPersona().setEdad(phNuevos.getPersona().getEdad());
      ph.setDireccion(phNuevos.getDireccion());
    } else if (existente instanceof Juridica pj && datosNuevos instanceof Juridica pjNuevos) {
      pj.setRazonSocial(pjNuevos.getRazonSocial());
      pj.setCuit(pjNuevos.getCuit());
      pj.setDireccion(pjNuevos.getDireccion());
    } else {
      throw new IllegalArgumentException("El tipo de persona no coincide con la entidad almacenada o es inválido.");
    }

    gestorDonantes.modificarDonante(id, existente);
    return existente;
  }

  public void eliminarPersona(UUID id) {
    gestorDonantes.darDeBajaDonante(id);
  }

  public String importarDonantes(MultipartFile file, List<MapeoCSV> mapeosCsv) {
    try {
      // 1. Extraemos los bytes del archivo para poder usarlo dentro del hilo asincrónico
      byte[] fileBytes = file.getBytes();

      // 2. Disparamos un hilo asincrónico para no bloquear al usuario
      CompletableFuture.runAsync(() -> {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
          // Instanciamos el conversor y el lector de forma temporal para esta única importación
          PersonaDonanteFilaConverter conversor = new PersonaDonanteFilaConverter(mapeosCsv);
          LectorCSV<Donante> lectorTemporal = new LectorCSV<>(',', conversor);

          // El LectorCSV internamente hace los loggers (warnings) de las filas que fallan
          List<Donante> donantesImportados = lectorTemporal.importar(inputStream);

          int guardados = 0;
          for (Donante donante : donantesImportados) {
            try {
              gestorDonantes.registrarDonante(donante);
              guardados++;
            } catch (Exception e) {
              System.err.println("Fallo al persistir un donante del CSV: " + e.getMessage());
            }
          }

          System.out.println("Importación CSV finalizada en 2do plano. Se procesaron y guardaron " + guardados + " donantes.");

        } catch (IOException e) {
          System.err.println("Error de IO al leer el stream del CSV: " + e.getMessage());
        }
      });

      // 3. Retornamos un string rápidamente mientras el hilo sigue trabajando
      return "El archivo fue recibido con éxito. La importación se está ejecutando en segundo plano y los errores de formato quedarán en los logs del servidor.";

    } catch (IOException e) {
      throw new RuntimeException("Error al acceder al archivo CSV", e);
    }
  }


  // --- GESTIÓN DE MEDIOS DE CONTACTO ---

  public MediosDeContacto obtenerMediosContacto(UUID id) {
    Donante persona = gestorDonantes.obtenerDonante(id);
    if (persona == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }

    return persona.getMediosDeContacto();
  }

  public Donante agregarMedioContacto(UUID id, MedioDeContacto nuevoMedio) {
    Donante persona = gestorDonantes.obtenerDonante(id);
    if (persona == null) {
      throw new IllegalArgumentException("No se encontró la persona con ID: " + id);
    }

    persona.agregarMedioDeContacto(nuevoMedio);
    gestorDonantes.modificarDonante(id, persona);
    return persona;
  }

  // --- AUTOMATION ---

  public void revisarActividades(){
    List<Donante> personas = gestorDonantes.listarTodosLosDonantes();
    for (Donante persona : personas) {
      revisarActividadPersona(persona);
    }
  }

  private void revisarActividadPersona(Donante persona) {
    // Validamos que tenga formularios antes de chequear inactividad
    if (persona.getFormularios() != null && !persona.getFormularios().isEmpty()) {
      if (persona.getFormularios().getLast().getFechaRealizacion().plusDays(20).isBefore(LocalDate.now())) {
        fabricaEstrategias.obtenerEstrategia(TipoEventoNotificacion.INACTIVIDAD_PERSONA_DONANTE).ejecutar(persona);
      }
    }
  }

}