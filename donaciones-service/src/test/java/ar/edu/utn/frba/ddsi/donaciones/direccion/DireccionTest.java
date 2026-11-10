package ar.edu.utn.frba.ddsi.donaciones.direccion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DireccionTest {
  private final Pais pais = new Pais("Argentina");
  private final Provincia provincia = new Provincia("CABA", pais);
  private final Ciudad ciudad = new Ciudad("Buenos Aires", provincia);

  @Test
  @DisplayName("Test se crea direccion correctamente")
  void test_direccionCorrecta() {
    Direccion direccion = new Direccion(
            "Av. Medrano",    // calleUno
            "Lavalle",        // calleDos
            951,              // altura (Integer)
            2,                // piso
            "A",              // departamento
            ciudad            // ciudad
    );

    assertEquals("Av. Medrano", direccion.getCalleUno());
    assertEquals("Lavalle", direccion.getCalleDos());
    assertEquals(951, direccion.getAltura());
    assertEquals(2, direccion.getPiso());
    assertEquals("A", direccion.getDepartamento());
    assertEquals(ciudad, direccion.getCiudad());
  }

  @Test
  @DisplayName("getDireccion() devuelve el string formateado correctamente con el número de altura")
  void test_getDireccion_ConAltura() {
    Direccion direccion = new Direccion(
            "Av. Medrano",
            "Lavalle",
            951,
            2,
            "A",
            ciudad
    );

    String resultado = direccion.getDireccion();

    String esperado = "Av. Medrano 951 y Lavalle, Piso 2, Depto A, " + ciudad.getDireccion();
    assertEquals(esperado, resultado);
  }

  @Test
  @DisplayName("getDireccion() devuelve el string con 'S/N' cuando se usa el constructor sin altura")
  void test_getDireccion_SinAltura() {
    Direccion direccion = new Direccion(
            "Av. Medrano",
            "Lavalle",
            2,
            "A",
            ciudad
    );

    String resultado = direccion.getDireccion();

    String esperado = "Av. Medrano S/N y Lavalle, Piso 2, Depto A, " + ciudad.getDireccion();
    assertEquals(esperado, resultado);
  }

  @Test
  @DisplayName("Punto 75: las coordenadas arrancan nulas y se pueden asignar")
  void test_coordenadas() {
    Direccion direccion = new Direccion("Av. Medrano", "Lavalle", 951, 2, "A", ciudad);

    assertFalse(direccion.tieneCoordenadas());

    direccion.setLatitud(-34.6037);
    direccion.setLongitud(-58.3816);

    assertTrue(direccion.tieneCoordenadas());
    assertEquals(-34.6037, direccion.getLatitud());
    assertEquals(-58.3816, direccion.getLongitud());
  }

  @Test
  @DisplayName("Punto 75: mismaDireccion ignora mayúsculas y espacios")
  void test_mismaDireccion_normaliza() {
    Direccion original = new Direccion("Av. Medrano", "Lavalle", 951, 2, "A", ciudad);
    Direccion igual = new Direccion("  av. medrano ", "LAVALLE", 951, 2, " a ", ciudad);

    assertTrue(original.mismaDireccion(igual));
  }

  @Test
  @DisplayName("Punto 75: mismaDireccion detecta un cambio de altura")
  void test_mismaDireccion_detectaCambio() {
    Direccion original = new Direccion("Av. Medrano", "Lavalle", 951, 2, "A", ciudad);
    Direccion otraAltura = new Direccion("Av. Medrano", "Lavalle", 952, 2, "A", ciudad);

    assertFalse(original.mismaDireccion(otraAltura));
  }

  @Test
  @DisplayName("Punto 75: anonimizar borra coordenadas e intentos de geocoding")
  void test_anonimizar_borraCoordenadas() {
    Direccion direccion = new Direccion("Av. Medrano", "Lavalle", 951, 2, "A", ciudad);
    direccion.setLatitud(-34.6037);
    direccion.setLongitud(-58.3816);
    direccion.setIntentosGeocoding(2);
    direccion.setUltimoIntentoGeocoding(java.time.LocalDateTime.now());

    direccion.anonimizar();

    assertNull(direccion.getCalleUno());
    assertFalse(direccion.tieneCoordenadas());
    assertNull(direccion.getLatitud());
    assertNull(direccion.getLongitud());
    assertEquals(0, direccion.getIntentosGeocoding());
    assertNull(direccion.getUltimoIntentoGeocoding());
  }
}