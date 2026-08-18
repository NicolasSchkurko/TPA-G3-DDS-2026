package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Conversor especializado en transformar una fila de CSV genérica en una PersonaDonante
 */
public class PersonaDonanteFilaConverter implements FilaConverter<Donante> {

  private static final Logger logger = Logger.getLogger(PersonaDonanteFilaConverter.class.getName());

  // Definición de campos lógicos que se esperan en el CSV, independientemente de cómo se llamen las columnas
  public enum CampoLogico {
    TIPO_PERSONA, TIPO_DOC, DOCUMENTO, NOMBRE_RAZON_SOCIAL, EMAIL, TELEFONO, WHATSAPP
  }

  private final Map<CampoLogico, List<String>> mapeoColumnas;

  /**
   * Constructor que recibe una lista de mapeos entre campos lógicos y nombres de columnas en el CSV.
   * @param mapeosCsv
   */
  public PersonaDonanteFilaConverter(List<MapeoCSV> mapeosCsv) {
    if (mapeosCsv == null || mapeosCsv.isEmpty()) {
      throw new IllegalArgumentException("Se requiere una lista de MapeoCSV para saber cómo leer el archivo.");
    }

    this.mapeoColumnas = new HashMap<>();
    System.out.println("DEBUG: Inicializando conversor con " + mapeosCsv.size() + " mapeos");

    for (MapeoCSV mapeo : mapeosCsv) {
      System.out.println("DEBUG: Procesando mapeo con campo: '" + mapeo.getCampo() + "'");
      try {
        CampoLogico campo = CampoLogico.valueOf(mapeo.getCampo().toUpperCase());
        this.mapeoColumnas.put(campo, mapeo.getNombresColumnas());
        System.out.println("DEBUG: Mapeo aceptado para campo: " + campo + " con columnas: " + mapeo.getNombresColumnas());
      } catch (IllegalArgumentException e) {
        System.out.println("DEBUG: Campo no reconocido: '" + mapeo.getCampo() + "' - Error: " + e.getMessage());
        logger.warning("Campo mapeado no reconocido en el sistema y será ignorado: " + mapeo.getCampo());
      }
    }
    System.out.println("DEBUG: Total de mapeos aceptados: " + mapeoColumnas.size());
    System.out.println("DEBUG: Mapeos finales: " + mapeoColumnas.keySet());
  }

  /**
   * Convierte una fila de CSV (representada como un Map) en una PersonaDonante (juridica o Humana).
   * @param fila Un mapa donde la clave es el nombre de la columna y el valor es el dato de la celda.
   * @return
   */
  @Override
  public Donante convertir(Map<String, String> fila) {
    System.out.println("DEBUG CONVERTIR: Claves en la fila: " + fila.keySet());

    String tipoPersona = obtenerPrimerValor(fila, CampoLogico.TIPO_PERSONA).orElse("").trim().toUpperCase();
    System.out.println("DEBUG CONVERTIR: tipoPersona extraído: '" + tipoPersona + "'");

    String tipoDoc = obtenerPrimerValor(fila, CampoLogico.TIPO_DOC).orElse("");
    String documento = obtenerPrimerValor(fila, CampoLogico.DOCUMENTO).orElse("");
    String nombreRazonSocial = obtenerValorConcatenado(fila, CampoLogico.NOMBRE_RAZON_SOCIAL);

    Donante donante = instanciarDonante(tipoPersona, nombreRazonSocial, tipoDoc, documento);
    vincularMediosDeContacto(donante, fila);

    return donante;
  }

  // --- Métodos de Lógica de Negocio ---

  /**
   * Instancia al PersonaDonante según el tipo de persona indicado en el CSV.
   * @param tipoPersona
   * @param nombreRazonSocial
   * @param tipoDoc
   * @param documento
   * @return
   */
  private Donante instanciarDonante(String tipoPersona, String nombreRazonSocial, String tipoDoc, String documento) {
    switch (tipoPersona) {
      case "HUMANA":
        return crearPersonaHumana(nombreRazonSocial, tipoDoc, documento);
      case "JURIDICA":
        return crearPersonaJuridica(nombreRazonSocial, tipoDoc, documento);
      default:
        throw new IllegalArgumentException("Tipo de persona no soportado: " + tipoPersona);
    }
  }

  /**
   * Crea una PersonaHumana a partir de los datos extraídos del CSV.
   * Se asume que el nombre completo se encuentra en un solo campo y se separa en nombre y apellido.
   * @param nombreCompleto
   * @param tipoDoc
   * @param documento
   * @return
   */
  private PersonaHumana crearPersonaHumana(String nombreCompleto, String tipoDoc, String documento) {
    String[] nombreYApellido = separarNombreYApellido(nombreCompleto);
    String nombre = nombreYApellido[0];
    String apellido = nombreYApellido[1];

    int numeroDocumentoParseado = limpiarYParsearDocumento(documento);

    Humana humana = new Humana(nombre, apellido, 0, numeroDocumentoParseado, null);
    return new PersonaHumana(humana, null, null);
  }

  /**
   * Crea una PersonaJuridica a partir de los datos extraídos del CSV.
   * Se asume que la razón social se encuentra en un solo campo.
   * @param razonSocial
   * @param tipoDoc
   * @param documento
   * @return
   */
  private Juridica crearPersonaJuridica(String razonSocial, String tipoDoc, String documento) {
    return new Juridica(null, razonSocial, null, null, documento, new ArrayList<>(), null);
  }

  /**
   * Vincula los medios de contacto (email y teléfono) a la PersonaDonante, si es que existen en la fila del CSV.
   * Se asume que el telefono es un medio de contacto separado al de WhatsApp
   * @param donante
   * @param fila
   */
  private void vincularMediosDeContacto(Donante donante, Map<String, String> fila) {
    String email = obtenerPrimerValor(fila, CampoLogico.EMAIL).orElse("");
    String telefono = obtenerPrimerValor(fila, CampoLogico.TELEFONO).orElse("");
    String whatsapp = obtenerPrimerValor(fila, CampoLogico.WHATSAPP).orElse("");

    if (email.isEmpty() && telefono.isEmpty()) {
      return; // No hay contactos para agregar
    }

    MediosDeContacto medios = new MediosDeContacto();

    if (!email.isEmpty()) {
      medios.agregarMedioDeContacto(new Mail(email));
    }

    if (!telefono.isEmpty()) {
      medios.agregarMedioDeContacto(new Telefono(telefono));
    }

    if (!whatsapp.isEmpty()) {
      medios.agregarMedioDeContacto(new Whatsapp(whatsapp));
    }

    donante.setMediosDeContacto(medios);
  }

  // --- Métodos Utilitarios Internos ---

  /**
   * Separa un nombre completo en nombre y apellido, asumiendo que el apellido es la última palabra.
   * Si no hay espacios, se asume que tdo es el nombre y el apellido queda vacío.
   * @param nombreCompleto
   * @return Un array de dos elementos: [nombre, apellido]
   */
  private String[] separarNombreYApellido(String nombreCompleto) {
    if (nombreCompleto == null || !nombreCompleto.contains(" ")) {
      return new String[]{nombreCompleto, ""}; // Asume que todo es nombre si no hay espacios
    }

    int indiceUltimoEspacio = nombreCompleto.lastIndexOf(" ");
    String nombre = nombreCompleto.substring(0, indiceUltimoEspacio).trim();
    String apellido = nombreCompleto.substring(indiceUltimoEspacio + 1).trim();

    return new String[]{nombre, apellido};
  }

  /**
   * Limpia el campo de documento eliminando cualquier carácter que no sea un dígito y luego intenta parsearlo a un entero.
   * Si el campo está vacío o no contiene números válidos, devuelve 0 como valor por defecto
   * (Revisar a futuro si vamos a usar DNI como PK)
   * @param documento
   * @return
   */
  private int limpiarYParsearDocumento(String documento) {
    try {
      String documentoSoloNumeros = documento.replaceAll("[^\\d]", "");
      if (!documentoSoloNumeros.isEmpty()) {
        return Integer.parseInt(documentoSoloNumeros);
      }
    } catch (NumberFormatException e) {
      logger.warning("No se pudo extraer un número válido del documento: " + documento);
    }
    return 0; // Valor por defecto si falla el parseo
  }

  /**
   * Obtiene el valor concatenado de todas las columnas posibles para un campo lógico dado, separando los valores por espacios.
   * Limpia el BOM (Byte Order Mark) de las claves de la fila.
   * @param fila
   * @param claveLogica
   * @return
   */
  private String obtenerValorConcatenado(Map<String, String> fila, CampoLogico claveLogica) {
    List<String> posiblesColumnasCSV = mapeoColumnas.get(claveLogica);
    if (posiblesColumnasCSV == null) {
      System.out.println("DEBUG obtenerValorConcatenado: No hay mapeo para " + claveLogica);
      return "";
    }

    // Limpiar BOM de las claves de la fila
    Map<String, String> filaLimpia = new HashMap<>();
    for (Map.Entry<String, String> entry : fila.entrySet()) {
      String llave = entry.getKey();
      if (llave.startsWith("\uFEFF")) {
        llave = llave.substring(1);
      }
      filaLimpia.put(llave, entry.getValue());
    }

    return posiblesColumnasCSV.stream()
                              .map(filaLimpia::get)
                              .filter(valor -> valor != null && !valor.isBlank())
                              .collect(Collectors.joining(" "))
                              .trim();
  }

  /**
   * Obtiene el primer valor no nulo y no vacío de las columnas posibles para un campo lógico dado.
   * Limpia el BOM (Byte Order Mark) de las claves de la fila.
   * @param fila
   * @param claveLogica
   * @return
   */
  private Optional<String> obtenerPrimerValor(Map<String, String> fila, CampoLogico claveLogica) {
    List<String> posiblesColumnasCSV = mapeoColumnas.get(claveLogica);
    if (posiblesColumnasCSV == null) {
      System.out.println("DEBUG obtenerPrimerValor: No hay mapeo para " + claveLogica);
      return Optional.empty();
    }

    // Limpiar BOM de las claves de la fila
    Map<String, String> filaLimpia = new HashMap<>();
    for (Map.Entry<String, String> entry : fila.entrySet()) {
      String llave = entry.getKey();
      if (llave.startsWith("\uFEFF")) {
        llave = llave.substring(1);
      }
      filaLimpia.put(llave, entry.getValue());
    }

    return posiblesColumnasCSV.stream()
                              .map(filaLimpia::get)
                              .filter(valor -> valor != null && !valor.isBlank())
                              .findFirst();
  }
}