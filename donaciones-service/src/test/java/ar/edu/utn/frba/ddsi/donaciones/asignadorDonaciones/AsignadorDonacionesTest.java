package ar.edu.utn.frba.ddsi.donaciones.asignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AsignadorDonacionesTest {

  @Test
  void constructorDebeInicializarListasVacias() {
    AsignadorDonaciones asignador = new AsignadorDonaciones();

    assertNotNull(asignador.getAlgoritmos());
    assertNotNull(asignador.getDonacionesPendientesDeAprobacion());

    assertTrue(asignador.getAlgoritmos().isEmpty());
    assertTrue(asignador.getDonacionesPendientesDeAprobacion().isEmpty());
  }
}
