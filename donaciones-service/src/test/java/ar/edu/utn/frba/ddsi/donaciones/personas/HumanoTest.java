package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Humano;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class HumanoTest {


    @Test
    @DisplayName("Test se crea un humano correctamente")
    void crearHumanoTest() {
        String nombre = "Nico";
        String apellido = "Scurchjcko";
        int edad = 27;
        int numeroDeDocumento = 1234567;
        Genero genero = Genero.OTRO;
        Humano humano = new Humano(nombre, apellido, edad, numeroDeDocumento, genero);
        assertEquals(nombre, humano.getNombre());
        assertEquals(apellido, humano.getApellido());
        assertEquals(edad, humano.getEdad());
        assertEquals(numeroDeDocumento, humano.getNumeroDeDocumento());
        assertEquals(genero, humano.getGenero());
    }

}
