package ar.edu.utn.frba.ddsi.donaciones.lector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.exceptions.csvexceptions.ArchivoCsvSinEncabezadosException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.csvexceptions.ConversorNuloException;
import ar.edu.utn.frba.ddsi.donaciones.exceptions.csvexceptions.EncabezadoCsvDuplicadoException;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.ResultadoLectura;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.FilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LectorCSV")
class LectorCSVTest {

  private final FilaConverter<String> mockConverter = fila -> {
    if (fila.containsKey("nombre") && fila.get("nombre").equals("DESCARTAR")) {
      throw new IllegalArgumentException("Nombre inválido: DESCARTAR");
    }
    return fila.get("nombre") + "-" + fila.get("edad");
  };

  @Nested
  @DisplayName("Parseo")
  class Parseo {

    @Test
    @DisplayName("importa un CSV bien formateado")
    void importarExitosamente() {
      LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
      ResultadoLectura<String> resultado = lector.importar(
          stream("Nombre,Edad\nJuan,30\nMaria,25"));

      assertEquals(2, resultado.getElementos().size());
      assertEquals("Juan-30", resultado.getElementos().get(0));
      assertEquals("Maria-25", resultado.getElementos().get(1));
      assertTrue(resultado.getErrores().isEmpty());
    }

    @Test
    @DisplayName("ignora las filas que el conversor descarta")
    void ignoraFilasDescartadasPorConversor() {
      LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
      ResultadoLectura<String> resultado = lector.importar(
          stream("Nombre,Edad\nJuan,30\nDESCARTAR,99\nPedro,40"));

      assertEquals(2, resultado.getElementos().size());
      assertEquals("Juan-30", resultado.getElementos().get(0));
      assertEquals("Pedro-40", resultado.getElementos().get(1));
      assertEquals(1, resultado.getErrores().size());
      assertTrue(resultado.getErrores().get(0).contains("DESCARTAR"));
    }

    @Test
    @DisplayName("lanza excepción si falta el encabezado")
    void lanzaExcepcionPorFaltaDeEncabezados() {
      LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
      assertThrows(ArchivoCsvSinEncabezadosException.class,
          () -> lector.importar(stream("")));
    }

    @Test
    @DisplayName("lanza excepción si hay encabezados duplicados")
    void lanzaExcepcionPorEncabezadosDuplicados() {
      LectorCSV<String> lector = new LectorCSV<>(',', mockConverter);
      assertThrows(EncabezadoCsvDuplicadoException.class,
          () -> lector.importar(stream("Nombre,Edad,Nombre\nJuan,30,Juan")));
    }

    @Test
    @DisplayName("lanza excepción si el conversor es null")
    void constructorLanzaExcepcionSiConversorEsNull() {
      assertThrows(ConversorNuloException.class, () -> new LectorCSV<>(',', null));
    }

    @Test
    @DisplayName("funciona con separador punto y coma")
    void funcionaConSeparadorDiferente() {
      LectorCSV<String> lector = new LectorCSV<>(';', mockConverter);
      ResultadoLectura<String> resultado = lector.importar(stream("Nombre;Edad\nCarlos;50"));

      assertEquals(1, resultado.getElementos().size());
      assertEquals("Carlos-50", resultado.getElementos().get(0));
    }
  }

  @Nested
  @DisplayName("Mapeos del front")
  class MapeosDelFront {

    @Test
    @DisplayName("resuelve columnas sin importar mayúsculas, espacios ni orden")
    void lasColumnasSeResuelvenAunConDiferenciasDeCasing() {
      LectorCSV<Donante> lector = new LectorCSV<>(',',
          new PersonaDonanteFilaConverter(mapeosDelFront()));
      ResultadoLectura<Donante> lectura = lector.importar(stream(
          "TipoPersona,Nombre Completo,Dni\r\nHUMANA,Sofia Garcia,30456789\r\n"));

      assertEquals(1, lectura.getElementos().size());
      assertEquals("Sofia Garcia", lectura.getElementos().get(0).getPersona().getNombreDeUsuario());
      assertTrue(lectura.getErrores().isEmpty());
    }

    @Test
    @DisplayName("con encabezados canónicos también importa")
    void conEncabezadosYaCanonicosImporta() {
      LectorCSV<Donante> lector = new LectorCSV<>(',',
          new PersonaDonanteFilaConverter(mapeosDelFront()));
      List<Donante> importados = lector.importar(stream(
          "tipopersona,nombre completo,dni\r\nHUMANA,Sofia Garcia,30456789\r\n")).getElementos();

      assertEquals(1, importados.size());
      assertEquals("Sofia Garcia", importados.get(0).getPersona().getNombreDeUsuario());
    }

    @Test
    @DisplayName("el modo streaming entrega las filas sin retenerlas")
    void streamingNoRetieneLasFilas() {
      LectorCSV<Donante> lector = new LectorCSV<>(',',
          new PersonaDonanteFilaConverter(mapeosDelFront()));
      List<Donante> vistos = new ArrayList<>();
      ResultadoLectura<Donante> lectura = lector.importar(stream(
          "TipoPersona,Nombre Completo,Dni\r\n"
              + "HUMANA,Sofia Garcia,30456789\r\n"
              + "HUMANA,Ana Navarro,31234567\r\n"), vistos::add);

      assertEquals(2, vistos.size());
      assertTrue(lectura.getElementos().isEmpty());
      assertEquals(2, lectura.getTotalFilas());
    }
  }

  @Nested
  @DisplayName("Carga real")
  class CargaReal {

    @Test
    @DisplayName("importa 500 filas con nombre y razón social completos")
    void laCargaRealImportaConNombresCompletos() {
      LectorCSV<Donante> lector = new LectorCSV<>(',',
          new PersonaDonanteFilaConverter(mapeosCargaReal()));
      ResultadoLectura<Donante> lectura = lector.importar(
          getClass().getResourceAsStream("/carga-real-a.csv"));
      List<Donante> importados = lectura.getElementos();

      assertEquals(499, importados.size());
      assertTrue(lectura.getErrores().isEmpty(), () -> "errores: " + lectura.getErrores());
      assertEquals(499, lectura.getTotalFilas());
      assertEquals(0, importados.stream()
          .filter(d -> d.getPersona().getNombreDeUsuario().isBlank())
          .count());
      assertEquals("Ana Navarro", importados.get(0).getPersona().getNombreDeUsuario());
      assertEquals("Santa Fe Industrial Fundación",
          importados.stream()
              .filter(d -> d.getPersona() instanceof Juridica)
              .findFirst().orElseThrow().getPersona().getNombreDeUsuario());
    }

    @Test
    @DisplayName("todos quedan con medio predeterminado")
    void todosQuedanConMedioPredeterminado() {
      LectorCSV<Donante> lector = new LectorCSV<>(',',
          new PersonaDonanteFilaConverter(mapeosCargaReal()));
      List<Donante> importados = lector.importar(
          getClass().getResourceAsStream("/carga-real-a.csv")).getElementos();

      long sinPredeterminado = importados.stream()
          .filter(d -> !d.getPersona().getMediosDeContacto().getListaMediosDeContacto().isEmpty())
          .filter(d -> d.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado() == null)
          .count();

      assertEquals(0, sinPredeterminado);
    }

    @Test
    @DisplayName("ningún donante queda con nombre de usuario en blanco")
    void ningunDonanteImportadoQuedaConNombreBlanco() {
      LectorCSV<Donante> lector = new LectorCSV<>(',',
          new PersonaDonanteFilaConverter(mapeosCargaReal()));
      List<Donante> importados = lector.importar(
          getClass().getResourceAsStream("/carga-real-a.csv")).getElementos();

      assertTrue(importados.size() > 400);
      assertEquals(0, importados.stream()
          .map(d -> d.getPersona().getNombreDeUsuario())
          .filter(String::isBlank)
          .count());
    }
  }

  private static InputStream stream(String contenido) {
    return new ByteArrayInputStream(contenido.getBytes(StandardCharsets.UTF_8));
  }

  private static List<MapeoCSV> mapeosDelFront() {
    return List.of(
        new MapeoCSV("TIPO_PERSONA", List.of("tipopersona")),
        new MapeoCSV("NOMBRE_RAZON_SOCIAL", List.of("nombre completo")),
        new MapeoCSV("DOCUMENTO", List.of("dni")));
  }

  private static List<MapeoCSV> mapeosCargaReal() {
    return List.of(
        new MapeoCSV("TIPO_PERSONA", List.of("TipoPersona")),
        new MapeoCSV("TIPO_DOC", List.of("TipoDoc")),
        new MapeoCSV("DOCUMENTO", List.of("Documento")),
        new MapeoCSV("NOMBRE_RAZON_SOCIAL", List.of("Nombre/Razón Social")),
        new MapeoCSV("EMAIL", List.of("Email")),
        new MapeoCSV("TELEFONO", List.of("Teléfono")));
  }
}
