package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Humano;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Conversor especializado en transformar una fila de CSV genérica
 */
public class PersonaDonanteFilaConverter implements FilaConverter<PersonaDonante> {

  private static final Logger logger = Logger.getLogger(PersonaDonanteFilaConverter.class.getName());

  public static final String CAMPO_TIPO_PERSONA = "TIPO_PERSONA";
  public static final String CAMPO_TIPO_DOC = "TIPO_DOC";
  public static final String CAMPO_DOCUMENTO = "DOCUMENTO";
  public static final String CAMPO_NOMBRE_RAZON = "NOMBRE_RAZON_SOCIAL";
  public static final String CAMPO_EMAIL = "EMAIL";
  public static final String CAMPO_TELEFONO = "TELEFONO";

  private final Map<String, List<String>> mapeoColumnas;

  private final Map<String, PersonaCreator> creadoresDePersona;

  @FunctionalInterface
  private interface PersonaCreator {
    PersonaDonante crear(String nombreRazonSocial, String tipoDoc, String documento);
  }

  public PersonaDonanteFilaConverter(Map<String, List<String>> mapeoColumnas) {
    if (mapeoColumnas == null || mapeoColumnas.isEmpty()) {
      throw new IllegalArgumentException("El mapeo de columnas es obligatorio.");
    }
    this.mapeoColumnas = new HashMap<>(mapeoColumnas);

    // Inicializamos y registramos los constructores
    this.creadoresDePersona = new HashMap<>();
    this.creadoresDePersona.put("HUMANA", this::crearPersonaHumana);
    this.creadoresDePersona.put("JURIDICA", this::crearPersonaJuridica);
  }

  @Override
  public PersonaDonante convert(Map<String, String> fila) {
    String tipoPersona = obtenerPrimerValor(fila, CAMPO_TIPO_PERSONA).orElse("").trim().toUpperCase();

    if (!creadoresDePersona.containsKey(tipoPersona)) {
      logger.warning("Fila ignorada: Tipo de persona no reconocido o faltante -> " + tipoPersona);
      return null;
    }

    String tipoDoc = obtenerPrimerValor(fila, CAMPO_TIPO_DOC).orElse("");
    String documento = obtenerPrimerValor(fila, CAMPO_DOCUMENTO).orElse("");

    String nombreRazonSocial = obtenerValorConcatenado(fila, CAMPO_NOMBRE_RAZON);

    String email = obtenerPrimerValor(fila, CAMPO_EMAIL).orElse("");
    String telefono = obtenerPrimerValor(fila, CAMPO_TELEFONO).orElse("");

    // Delegamos la instanciación al Factory mapeado según el tipoPersona
    PersonaDonante donante = creadoresDePersona.get(tipoPersona).crear(nombreRazonSocial, tipoDoc, documento);

    asignarMediosDeContacto(donante, email, telefono);

    return donante;
  }

  private PersonaHumana crearPersonaHumana(String nombreCompleto, String tipoDoc, String documento) {
    String nombre = nombreCompleto;
    String apellido = "";

    // Separamos la última palabra como apellido
    if (nombreCompleto != null && nombreCompleto.contains(" ")) {
      int lastSpaceIndex = nombreCompleto.lastIndexOf(" ");
      nombre = nombreCompleto.substring(0, lastSpaceIndex).trim();
      apellido = nombreCompleto.substring(lastSpaceIndex + 1).trim();
    }

    int numeroDocumento = 0;
    try {
      String cleanDoc = documento.replaceAll("[^\\d]", "");
      if (!cleanDoc.isEmpty()) {
        numeroDocumento = Integer.parseInt(cleanDoc);
      }
    } catch (NumberFormatException e) {
      logger.warning("No se pudo parsear el documento a número: " + documento);
    }

    Humano humano = new Humano(nombre, apellido, 0, numeroDocumento, null);
    return new PersonaHumana(humano, null);
  }

  private PersonaJuridica crearPersonaJuridica(String razonSocial, String tipoDoc, String documento) {
    return new PersonaJuridica(
        null,
        razonSocial,
        null,
        null,
        documento,
        new ArrayList<>()
    );
  }

  private void asignarMediosDeContacto(PersonaDonante donante, String email, String telefono) {
    if (email.isEmpty() && telefono.isEmpty()) {
      return;
    }

    MediosDeContacto medios = new MediosDeContacto();

    if (!email.isEmpty()) {
      Mail mail = new Mail(email);
      medios.agregarMedioDeContacto(mail);
    }

    if (!telefono.isEmpty()) {
      Telefono tel = new Telefono(telefono);
      medios.agregarMedioDeContacto(tel);
    }

    donante.setMediosDeContacto(medios);
  }

  /**
   * Concatena los valores de todas las columnas mapeadas para una clave logica.
   */
  private String obtenerValorConcatenado(Map<String, String> fila, String claveLogica) {
    List<String> posiblesColumnasCSV = mapeoColumnas.get(claveLogica);
    if (posiblesColumnasCSV == null) {
      return "";
    }
    return posiblesColumnasCSV.stream()
                              .map(fila::get)
                              .filter(s -> s != null && !s.isBlank())
                              .collect(Collectors.joining(" "))
                              .trim();
  }

  /**
   * Busca el primer valor no vacioo en la fila a partir de las posibles columnas mapeadas.
   */
  private Optional<String> obtenerPrimerValor(Map<String, String> fila, String claveLogica) {
    List<String> posiblesColumnasCSV = mapeoColumnas.get(claveLogica);
    if (posiblesColumnasCSV == null) {
      return Optional.empty();
    }
    return posiblesColumnasCSV.stream()
                              .map(fila::get)
                              .filter(s -> s != null && !s.isBlank())
                              .findFirst();
  }
}