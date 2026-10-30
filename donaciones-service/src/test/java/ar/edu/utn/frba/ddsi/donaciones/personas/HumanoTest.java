package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HumanoTest {

  @Test
  @DisplayName("Test se crea un humano correctamente")
  void crearHumanoTest() {
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
    assertEquals("Nico Scurchjcko", humana.getNombreDeUsuario());
  }

  @Test
  @DisplayName("No permite crear una persona humana sin nombre ni apellido")
  void noPermiteCrearHumanaSinNombreNiApellido() {
    assertThrows(IllegalArgumentException.class, () ->
        new Humana(null, null, 25, 12345678, Genero.HOMBRE));
    assertThrows(IllegalArgumentException.class, () ->
        new Humana("   ", "", 25, 12345678, Genero.HOMBRE));
  }

  @Test
  @DisplayName("getNombreDeUsuario formatea correctamente sin generar nulls ni espacios vacíos")
  void getNombreDeUsuarioFormateaCorrectamente() {
    Humana soloNombre = new Humana("Juan", null, 30, 11111111, Genero.HOMBRE);
    assertEquals("Juan", soloNombre.getNombreDeUsuario());

    Humana soloApellido = new Humana(null, "Perez", 30, 22222222, Genero.HOMBRE);
    assertEquals("Perez", soloApellido.getNombreDeUsuario());

    Humana conEspacios = new Humana("  Maria  ", "  Gomez  ", 28, 33333333, Genero.MUJER);
    assertEquals("Maria Gomez", conEspacios.getNombreDeUsuario());
  }
}
