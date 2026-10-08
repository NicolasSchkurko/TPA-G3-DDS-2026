package ar.edu.utn.frba.ddsi.donaciones.lector;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.ResultadoLectura;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El mapeo del request nombra columnas que aterrizar contra los encabezados del CSV:
 * Excel acentúa, agrega espacios y usa mayúsculas a gusto, así que la búsqueda tiene que
 * normalizar de los dos lados.
 *
 * <p>Reproduce la carga real que rompió en producción: encabezados con otro casing y el
 * mapeo tal cual lo manda el front.
 */
public class ImportarCsvConMapeosTest {

  private List<MapeoCSV> mapeosDelFront() {
    return List.of(
        new MapeoCSV("TIPO_PERSONA", List.of("tipopersona")),
        new MapeoCSV("NOMBRE_RAZON_SOCIAL", List.of("nombre completo")),
        new MapeoCSV("DOCUMENTO", List.of("dni"))
    );
  }

  @Test
  @DisplayName("Las columnas del mapeo se resuelven sin importar mayúsculas, espacios ni orden")
  void lasColumnasSeResuelvenAunConDiferenciasDeCasing() {
    String csv = "TipoPersona,Nombre Completo,Dni\r\nHUMANA,Sofia Garcia,30456789\r\n";

    LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosDelFront()));
    ResultadoLectura<Donante> lectura = lector.importar(
            new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    List<Donante> importados = lectura.getElementos();

    assertEquals(1, importados.size(), "la fila tiene todos los datos: no puede descartarse");
    assertEquals("Sofia Garcia", importados.get(0).getPersona().getNombreDeUsuario(),
            "el nombre de usuario nunca puede salir en blanco para una fila con nombre real");
    assertTrue(lectura.getErrores().isEmpty(), () -> "errores: " + lectura.getErrores());
  }

  @Test
  @DisplayName("Con encabezados ya canónicos, la fila se importa")
  void conEncabezadosYaCanonicos_importa() {
    String csv = "tipopersona,nombre completo,dni\r\nHUMANA,Sofia Garcia,30456789\r\n";

    LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosDelFront()));
    List<Donante> importados = lector.importar(
            new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))).getElementos();

    assertEquals(1, importados.size(), "incluso con encabezados exactos tiene que importar");
    assertEquals("Sofia Garcia", importados.get(0).getPersona().getNombreDeUsuario());
  }

  @Test
  @DisplayName("Aislado del LectorCSV: el converter resuelve solo, con claves canónicas")
  void converterSolo_resuelveColumnas() {
    PersonaDonanteFilaConverter conv = new PersonaDonanteFilaConverter(mapeosDelFront());
    Donante donante = conv.convertir(java.util.Map.of(
            "tipopersona", "HUMANA",
            "nombre completo", "Sofia Garcia",
            "dni", "30456789"));

    assertEquals("Sofia Garcia", donante.getPersona().getNombreDeUsuario());
  }
}
