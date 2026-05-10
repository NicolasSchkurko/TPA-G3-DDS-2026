package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv;


import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.Lector;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.FilaConverter;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Lector de archivos CSV genérico
 * Delega la responsabilidad de convertir una fila en un objeto a un 'FilaConverter'.
 *
 * @param <T> El tipo de objeto a crear desde cada fila del CSV.
 */
public class LectorCSV<T> implements Lector<T> {

  private static final Logger logger = Logger.getLogger(LectorCSV.class.getName());

  private final char separator;
  private final FilaConverter<T> converter;

  public LectorCSV(char separator, FilaConverter<T> converter) {
    if (converter == null) {
      throw new IllegalArgumentException("El conversor no puede ser nulo.");
    }
    this.separator = separator;
    this.converter = (FilaConverter<T>) converter;
  }

  @Override
  public List<T> importar(InputStream contenido) {
    List<T> resultados = new ArrayList<>();
    try (
        // Usamos el inputStream directamente en lugar de un FileInputStream
        CSVReader reader = new CSVReaderBuilder(
            new InputStreamReader(contenido, StandardCharsets.UTF_8)
        ).withCSVParser(new CSVParserBuilder().withSeparator(separator)
                                              .build())
         .build()
    ) {
      String[] headers = reader.readNext();
      if (headers == null || headers.length == 0) {
        throw new IllegalArgumentException("El archivo CSV no contiene encabezados.");
      }

      String[] row;
      int linea = 1;

      while ((row = reader.readNext()) != null) {
        linea++;
        Map<String, String> filaMapeada = mapearFila(row, headers);
        T objeto = converter.convert(Collections.unmodifiableMap(filaMapeada));

        if (objeto != null) {
          resultados.add(objeto);
        } else {
          logger.warning("[Línea " + linea + "] Fila descartada por el conversor.");
        }
      }
    } catch (IOException | CsvException e) {
      throw new RuntimeException("Error al leer el stream CSV", e);
    }
    return resultados;
  }

  /**
   * Mapea una fila del CSV a un mapa de [nombre de columna -> valor].
   *
   * @param row     Array de strings que representa una fila.
   * @param headers Array de strings que representa los encabezados.
   * @return Un mapa que representa la fila.
   */
  private Map<String, String> mapearFila(String[] row, String[] headers) {
    Map<String, String> fila = new HashMap<>();
    Set<String> seenHeaders = new HashSet<>();

    for (int i = 0; i < headers.length; i++) {
      String header = headers[i].trim();
      if (seenHeaders.contains(header)) {
        throw new IllegalArgumentException("Encabezado duplicado detectado: " + header);
      }
      seenHeaders.add(header);

      if (i < row.length && row[i] != null) {
        fila.put(header, row[i].trim());
      } else {
        fila.put(header, null);
      }
    }
    return fila;
  }


}

