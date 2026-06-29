package ar.edu.utn.frba.ddsi.donaciones.necesidad;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.NecesidadRecurrente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;



public class NecesidadTest {
  CategoriaBien vestimenta = new CategoriaBien("Vestimenta");
  SubcategoriaBien subcategoria = new SubcategoriaBien("Ropa", vestimenta);

  @Test
  @DisplayName("Test se creo una necesidad recurrente correctamente")
  void necesidad_recurrente_correcta(){
    Necesidad necesidadRecurrente = new NecesidadRecurrente(
            subcategoria,
            "Necesitamos ropa de invierno para niños",
            100, 10);

    assertEquals(subcategoria, necesidadRecurrente.getSubcategoria());
    assertEquals(
            "Necesitamos ropa de invierno para niños",
            necesidadRecurrente.getDescripcion()
    );
    assertEquals(100, necesidadRecurrente.getCantidadObjetivo());

    NecesidadRecurrente recurrente =
            (NecesidadRecurrente) necesidadRecurrente;

    assertEquals(10, recurrente.getPlazoEnDias());

    assertTrue(necesidadRecurrente.getDonaciones().isEmpty());
  }

  @Test
  @DisplayName("Test se creo una necesidad extraordinaria correctamente")
  void necesidad_extraordinaria_correcta() {
    Necesidad necesidadExtraordinaria = new NecesidadExtraordinaria(
            subcategoria,
            "Necesitamos ropa de invierno para niños",
            100);
    assertEquals(subcategoria, necesidadExtraordinaria.getSubcategoria());
    assertEquals(
            "Necesitamos ropa de invierno para niños",
            necesidadExtraordinaria.getDescripcion()
    );
    assertEquals(100, necesidadExtraordinaria.getCantidadObjetivo());

    assertTrue(necesidadExtraordinaria.getDonaciones().isEmpty());
  }

  @Test
  @DisplayName("Una necesidad extraordinaria no esta satisfecha sin donaciones")
  void necesidad_extraordinaria_no_satisfecha() {
    Necesidad necesidadExtraordinaria = new NecesidadExtraordinaria(
            subcategoria,
            "Necesitamos ropa de invierno para niños",
            100);
    assertFalse(necesidadExtraordinaria.estaSatisfecha());
  }

  @Test
  @DisplayName("Una necesidad recurrente no esta satisfecha sin donaciones")
  void necesidad_recurrente_no_satisfecha() {
    Necesidad necesidadRecurrente = new NecesidadRecurrente(
            subcategoria,
            "Necesitamos ropa de invierno para niños",
            100, 10);
    assertFalse(necesidadRecurrente.estaSatisfecha());
  }

}
