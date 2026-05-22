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
}