package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv;

import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.ArchivoCsvSinEncabezadosException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.ConversorNuloException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.EncabezadoCsvDuplicadoException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.ErrorAlLeerCsvException;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.Lector;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.FilaConverter;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

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

  private final char separador;
  private final FilaConverter<T> conversor;

  public LectorCSV(char separador, FilaConverter<T> conversor) {
    if (conversor == null) {
      throw new ConversorNuloException("Para leer el CSV se requiere un conversor de filas válido.");
    }
    this.separador = separador;
    this.conversor = conversor;
  }

  /**
   * Importa el contenido de un archivo CSV y lo convierte en una lista de objetos del tipo T.
   */
  @Override
  public List<T> importar(InputStream contenido) {
    List<T> resultados = new ArrayList<>();

    try (CSVReader lectorDeArchivo = inicializarLectorDeArchivo(contenido)) {

      String[] encabezados = lectorDeArchivo.readNext();
      validarQueExistanEncabezados(encabezados);

      String[] valoresFila;
      int numeroLinea = 1;

      while ((valoresFila = lectorDeArchivo.readNext()) != null) {
        numeroLinea++;
        procesarYGuardarFila(valoresFila, encabezados, numeroLinea, resultados);
      }

    } catch (IOException | CsvException e) {
      throw new ErrorAlLeerCsvException("Ocurrió un problema inesperado al leer el contenido del CSV", e);
    }

    return resultados;
  }

  /**
   * Inicializa un CSVReader con el separador configurado y asegurando la lectura en UTF-8.
   * Se utiliza InputStreamReader para garantizar que el contenido se interprete correctamente como texto.
   */
  private CSVReader inicializarLectorDeArchivo(InputStream contenido) {
    return new CSVReaderBuilder(new InputStreamReader(contenido, StandardCharsets.UTF_8))
        .withCSVParser(new CSVParserBuilder().withSeparator(this.separador).build())
        .build();
  }

  /**
   * Valida la existencia de encabezados en el csv a importar.
   */
  private void validarQueExistanEncabezados(String[] encabezados) {
    if (encabezados == null || encabezados.length == 0) {
      throw new ArchivoCsvSinEncabezadosException("El archivo CSV debe tener una primera fila con los títulos de las columnas.");
    }
  }

  /**
   * Procesa una fila del CSV, vinculando cada valor con su encabezado correspondiente.
   * Luego utiliza el conversor para crear un objeto del tipo T.
   */
  private void procesarYGuardarFila(String[] valoresFila, String[] encabezados, int numeroLinea, List<T> resultados) {
    Map<String, String> filaMapeada = vincularEncabezadosConValores(valoresFila, encabezados);

    try {
      T objetoConvertido = conversor.convertir(Collections.unmodifiableMap(filaMapeada));
      resultados.add(objetoConvertido);
    } catch (Exception e) {
      logger.warning(String.format("[Línea %d] Fila descartada: Error al convertir fila - %s", numeroLinea, e.getMessage()));
    }
  }

  /**
   * Vincula cada valor de la fila con su encabezado correspondiente.
   */
  private Map<String, String> vincularEncabezadosConValores(String[] valoresFila, String[] encabezados) {
    Map<String, String> filaVinculada = new HashMap<>();
    Set<String> encabezadosYaVistos = new HashSet<>();

    for (int indiceColumna = 0; indiceColumna < encabezados.length; indiceColumna++) {
      String nombreEncabezado = encabezados[indiceColumna].trim();

      validarEncabezadoNoDuplicado(encabezadosYaVistos, nombreEncabezado);

      String valorCelda = extraerValorDeCelda(valoresFila, indiceColumna);
      filaVinculada.put(nombreEncabezado, valorCelda);
    }

    return filaVinculada;
  }

  /**
   * Valida que no existan encabezados duplicados.
   */
  private void validarEncabezadoNoDuplicado(Set<String> encabezadosYaVistos, String nombreEncabezado) {
    if (encabezadosYaVistos.contains(nombreEncabezado)) {
      throw new EncabezadoCsvDuplicadoException(nombreEncabezado);
    }
    encabezadosYaVistos.add(nombreEncabezado);
  }

  /**
   * Extrae el valor de una celda dada su posición en la fila, asegurando que no se intente acceder a un índice fuera de rango.
   * Si no existe un valor para esa columna, se devuelve null.
   */
  private String extraerValorDeCelda(String[] valoresFila, int indiceColumna) {
    boolean existeValorParaEstaColumna = indiceColumna < valoresFila.length && valoresFila[indiceColumna] != null;
    if (existeValorParaEstaColumna) {
      return valoresFila[indiceColumna].trim();
    }
    return null;
  }
}