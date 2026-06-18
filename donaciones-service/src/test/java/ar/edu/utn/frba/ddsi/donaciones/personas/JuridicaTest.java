package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class JuridicaTest {
    @Test
    public void crearPersonaJuridicaConTodosLosValores() {
        // Arrange
        Ciudad ciudad = new Ciudad("Buenos Aires", new Provincia("Buenos Aires", new Pais("Argentina")));
        Direccion direccion = new Direccion("Rivadavia", "Corrientes", 1234, 2, "A", ciudad);

        Humano humanoRep = new Humano("Maria", "Lopez", 40, 87654321, Genero.MUJER);
        Representante representante = new Representante(humanoRep, true);

        PersonaJuridica persona = new PersonaJuridica(
                direccion,
                "Fundacion Ayuda",
                "ONGs",
                TipoJuridico.ONG,
                "20-12345678-9",
                List.of(representante)
        );


        // Assert
        assertEquals("Fundacion Ayuda", persona.getRazonSocial());
        assertEquals("ONGs", persona.getRubro());
        assertEquals(TipoJuridico.ONG, persona.getTipoJuridico());
        assertEquals("20-12345678-9", persona.getCuit());
        assertEquals(1, persona.getRepresentantes().size());
        assertTrue(persona.getRepresentantes().contains(representante));
        assertEquals("Fundacion Ayuda", persona.darNombre());
    }
    @Test
    public void crearPersonaHumanaConTodosLosValores() {
        // Arrange
        Humano humano = new Humano("Juan", "Perez", 30, 12345678, Genero.HOMBRE);
        Ciudad ciudad = new Ciudad("Buenos Aires", new Provincia("Buenos Aires", new Pais("Argentina")));
        Direccion direccion = new Direccion("Rivadavia", "Corrientes", 1234, 2, "A", ciudad);

        PersonaHumana persona = new PersonaHumana(humano, direccion);


        assertEquals("Juan", persona.getPersona().getNombre());
        assertEquals("Perez", persona.getPersona().getApellido());
        assertEquals(30, persona.getPersona().getEdad());
        assertEquals(12345678, persona.getPersona().getNumeroDeDocumento());
        assertEquals(Genero.HOMBRE, persona.getPersona().getGenero());
        assertEquals("Juan Perez", persona.darNombre());
    }
}
