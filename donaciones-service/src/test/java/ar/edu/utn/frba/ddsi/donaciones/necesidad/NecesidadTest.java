package ar.edu.utn.frba.ddsi.donaciones.necesidad;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class NecesidadTest {
  NecesidadRecurrente necesidadRecurrente = new Necesidad(new SubcategoriaBien("Ropa", CategoriaBien.VESTIMENTA), "Necesitamos ropa de invierno para niños", 100, 10);

  @Test
  @DisplayName("Test ")

}
