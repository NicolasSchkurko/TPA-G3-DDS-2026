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
}