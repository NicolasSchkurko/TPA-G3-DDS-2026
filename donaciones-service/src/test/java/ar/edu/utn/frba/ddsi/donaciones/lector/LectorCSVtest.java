package ar.edu.utn.frba.ddsi.donaciones.lector;

import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.ArchivoCsvSinEncabezadosException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.ConversorNuloException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.CsvExceptions.EncabezadoCsvDuplicadoException;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.ResultadoLectura;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.FilaConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.boot.test.context.SpringBootTest;


public class LectorCSVtest {


  private final FilaConverter<String> mockConverter = fila -> {
    if (fila.containsKey("Nombre") && fila.get("Nombre").equals("DESCARTAR")) { //Si detecta DESCARTAR lanza excepcion
      throw new IllegalArgumentException("Nombre inválido: DESCARTAR");
    }
    return fila.get("Nombre") + "-" + fila.get("Edad"); //Sino devuelve nombre - edad
  };

  private InputStream crearStreamDesdeString(String contenido) {
    return new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Debe importar correctamente un CSV bien formateado")
  void importar_Exitosamente() {
    LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
    String csvContenido = "Nombre,Edad\nJuan,30\nMaria,25";
    InputStream inputStream = crearStreamDesdeString(csvContenido);

    ResultadoLectura<String> resultado = lector.importar(inputStream);

    assertEquals(2, resultado.getElementos().size());
    assertEquals("Juan-30", resultado.getElementos().get(0));
    assertEquals("Maria-25", resultado.getElementos().get(1));
    assertTrue(resultado.getErrores().isEmpty());
  }

  @Test
  @DisplayName("Debe ignorar las filas que el conversor descarta (lanzando excepción)")
  void importar_IgnoraFilasDescartadasPorConversor() {
    LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
    String csvContenido = "Nombre,Edad\nJuan,30\nDESCARTAR,99\nPedro,40";
    InputStream inputStream = crearStreamDesdeString(csvContenido);


    ResultadoLectura<String> resultado = lector.importar(inputStream);

    assertEquals(2, resultado.getElementos().size(), "Debería haber ignorado 1 fila");
    assertEquals("Juan-30", resultado.getElementos().get(0));
    assertEquals("Pedro-40", resultado.getElementos().get(1));
    assertEquals(1, resultado.getErrores().size(), "La fila descartada queda registrada como error");
    assertTrue(resultado.getErrores().get(0).contains("DESCARTAR"));
  }

  @Test
  @DisplayName("Debe lanzar excepción si el archivo CSV está completamente vacío (sin encabezados)")
  void importar_LanzaExcepcionPorFaltaDeEncabezados() {
    LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
    InputStream inputStream = crearStreamDesdeString("");

    ArchivoCsvSinEncabezadosException exception = assertThrows(
        ArchivoCsvSinEncabezadosException.class,
        () -> lector.importar(inputStream)
    );
    assertTrue(exception.getMessage().contains("El archivo CSV debe tener una primera fila con los títulos de las columnas"));
  }

  @Test
  @DisplayName("Debe lanzar excepción si el CSV tiene columnas duplicadas")
  void importar_LanzaExcepcionPorEncabezadosDuplicados() {

    LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
    String csvContenido = "Nombre,Edad,Nombre\nJuan,30,Juan";
    InputStream inputStream = crearStreamDesdeString(csvContenido);

    EncabezadoCsvDuplicadoException exception = assertThrows(
        EncabezadoCsvDuplicadoException.class,
        () -> lector.importar(inputStream)
    );
    assertTrue(exception.getMessage().contains("Se detectó un encabezado duplicado"));
  }

  @Test
  @DisplayName("Debe lanzar excepción si se intenta instanciar sin un conversor")
  void constructor_LanzaExcepcionSiConversorEsNull() {
    // Act & Assert
    ConversorNuloException exception = assertThrows(
        ConversorNuloException.class,
        () -> new LectorCSV<>(',', null)
    );
    assertTrue(exception.getMessage().contains("conversor"));
  }

  @Test
  @DisplayName("Debe funcionar correctamente usando un separador distinto (ej: punto y coma)")
  void importar_FuncionaConSeparadorDiferente() {

    LectorCSV<String> lector = new LectorCSV<>(';', mockConverter);
    String csvContenido = "Nombre;Edad\nCarlos;50";
    InputStream inputStream = crearStreamDesdeString(csvContenido);

    ResultadoLectura<String> resultado = lector.importar(inputStream);

    assertEquals(1, resultado.getElementos().size());
    assertEquals("Carlos-50", resultado.getElementos().get(0));
  }
}