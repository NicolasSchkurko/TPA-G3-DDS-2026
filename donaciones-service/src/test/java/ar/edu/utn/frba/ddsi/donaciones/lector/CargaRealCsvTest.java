package ar.edu.utn.frba.ddsi.donaciones.lector;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.ResultadoLectura;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.LectorCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Carga real (500 filas) con la que la integración falló en producción: sin nombre de
 * usuario, cada alta de perfil respondía 400 "El donante requiere un nombre de usuario".
 * El mapeodice las columnas tal como las manda el front y el CSV usa los encabezados
 * con tilde, barra y mayúsculas que trae Excel.
 */
public class CargaRealCsvTest {

  private List<MapeoCSV> mapeosDelFront() {
    return List.of(
        new MapeoCSV("TIPO_PERSONA", List.of("TipoPersona")),
        new MapeoCSV("TIPO_DOC", List.of("TipoDoc")),
        new MapeoCSV("DOCUMENTO", List.of("Documento")),
        new MapeoCSV("NOMBRE_RAZON_SOCIAL", List.of("Nombre/Razón Social")),
        new MapeoCSV("EMAIL", List.of("Email")),
        new MapeoCSV("TELEFONO", List.of("Teléfono"))
    );
  }

  @Test
  @DisplayName("La carga real de 500 filas importa donantes con nombre y razón social completos")
  void laCargaRealImportaConNombresCompletos() {
    LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosDelFront()));
    ResultadoLectura<Donante> lectura = lector.importar(getClass().getResourceAsStream("/carga-real-a.csv"));
    List<Donante> importados = lectura.getElementos();

    assertEquals(499, importados.size(), "1 encabezado + 499 filas: ninguna se descarta");
    assertTrue(lectura.getErrores().isEmpty(), () -> "errores: " + lectura.getErrores());
    assertEquals(499, lectura.getTotalFilas());

    long conNombreBlanco = importados.stream()
            .filter(d -> d.getPersona().getNombreDeUsuario().isBlank())
            .count();
    assertEquals(0, conNombreBlanco, "ningún donante puede nacer sin nombre de usuario");

    assertEquals("Ana Navarro", importados.get(0).getPersona().getNombreDeUsuario(), "primera fila Humana");
    assertEquals("Santa Fe Industrial Fundación",
            importados.stream().filter(d -> d.getPersona() instanceof ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica)
                    .findFirst().orElseThrow().getPersona().getNombreDeUsuario(),
            "primera fila Jurídica: razón social completa");
  }

  @Test
  @DisplayName("Ningún donante importado se va a rechazar por nombre de usuario en blanco")
  void ningunDonanteImportadoQuedaConNombreBlanco() {
    LectorCSV<Donante> lector = new LectorCSV<>(',', new PersonaDonanteFilaConverter(mapeosDelFront()));
    List<Donante> importados = lector.importar(getClass().getResourceAsStream("/carga-real-a.csv"))
            .getElementos();

    assertTrue(importados.size() > 400, "la carga trae muchas filas");
    long nombresBlancos = importados.stream()
            .map(d -> d.getPersona().getNombreDeUsuario())
            .filter(String::isBlank)
            .count();
    assertEquals(0, nombresBlancos, "con nombres en blanco, incentivos responde 400 por fila");
  }
}
