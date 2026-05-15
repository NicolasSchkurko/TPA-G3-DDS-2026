package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter;

import ar.edu.utn.frba.ddsi.common.Persona;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Implementación de 'FilaConverter' específica para crear objetos 'Persona'.
 * Contiene toda la lógica de negocio para transformar una fila de CSV en un 'Persona'.
 */
public class PersonaFilaConverter implements FilaConverter<Persona> {

  private static final Logger logger = Logger.getLogger(PersonaFilaConverter.class.getName());

  private final Map<String, List<String>> mapeoColumnas;

  /**
   * Constructor que recibe la configuración necesaria para la conversión.
   *
   * @param mapeoColumnas Mapa que asocia un campo de 'Persona' con una o más columnas del CSV.
   */
  public PersonaFilaConverter(Map<String, List<String>> mapeoColumnas) {
    if (mapeoColumnas == null || mapeoColumnas.isEmpty()) {
      throw new IllegalArgumentException("El mapeo de columnas es obligatorio.");
    }
    this.mapeoColumnas = new HashMap<>(mapeoColumnas);
  }

  /**
   * Convierte una fila de CSV en un objeto Persona.
   *
   * @param fila Un mapa donde la clave es el nombre de la columna y el valor es el dato de la celda.
   * @return Un Persona, o null si la fila no es válida.
   */
  @Override
  public Persona convert(Map<String, String> fila) {
    if (!validadorDeFila(fila)) {
      logger.warning("Fila ignorada por no cumplir con los campos requeridos");
      return null;
    }

    String nombre = obtenerPrimerValor(fila, "NOMBRE").orElse(null);
    String apellido = obtenerPrimerValor(fila, "APELLIDO").orElse(null);

    if (nombre != null && apellido != null) {
      return new Persona(nombre, apellido);
    } else {
      logger.warning("Nombre o apellido faltante en la fila");
      return null;
    }
  }

  /**
   * Valida si una fila contiene todos los campos requeridos con valores no vacíos.
   *
   * @param fila La fila a validar.
   * @return true si la fila es válida, false en caso contrario.
   */
  private boolean validadorDeFila(Map<String, String> fila) {
    boolean nombrePresente = obtenerPrimerValor(fila, "NOMBRE").isPresent();
    boolean apellidoPresente = obtenerPrimerValor(fila, "APELLIDO").isPresent();

    if (!nombrePresente || !apellidoPresente) {
      logger.warning("Campos requeridos faltantes: NOMBRE o APELLIDO | Columnas disponibles: " + fila.keySet());
      return false;
    }

    return true;
  }

  private java.util.Optional<String> obtenerPrimerValor(Map<String, String> fila, String campo) {
    List<String> posiblesColumnas = mapeoColumnas.get(campo);
    if (posiblesColumnas == null) {
      return java.util.Optional.empty();
    }
    return posiblesColumnas.stream()
                           .map(fila::get)
                           .filter(s -> s != null && !s.isBlank())
                           .findFirst();
  }



}