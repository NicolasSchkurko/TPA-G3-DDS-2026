package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;



public class HumanoTest {


  @Test
  @DisplayName("Test se crea un humano correctamente")
  void crearHumanoTest(){
    String nombre = "Nico";
    String apellido = "Scurchjcko";
    int edad = 27;
    int numeroDeDocumento = 1234567;
    Genero genero = Genero.OTRO;
    Humana humana = new Humana(nombre, apellido, edad, numeroDeDocumento, genero);
    assertEquals(nombre, humana.getNombre());
    assertEquals(apellido, humana.getApellido());
    assertEquals(edad, humana.getEdad());
    assertEquals(numeroDeDocumento, humana.getNumeroDeDocumento());
    assertEquals(genero, humana.getGenero());
  }

}
