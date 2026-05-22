package ar.edu.utn.frba.ddsi.donaciones.direccion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DireccionTest {
  Ciudad ciudad = new Ciudad("Buenos Aires", "CABA", "Argentina");
  Direccion direccion = new Direccion("Av. Medrano ", di "Lavalle", 951, 2, "A", ciudad);


    @Test
    @DisplayName("Test se crea direccion correctamente")
    void test_dirrecionCorrecta (){
assertEquals()"Av.Medrano", direccion.getCalleUno()
;
    asserEquals()}
}
