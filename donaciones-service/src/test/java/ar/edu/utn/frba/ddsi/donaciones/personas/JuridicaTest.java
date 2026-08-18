package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class JuridicaTest {
    @Test
    public void crearPersonaJuridicaConTodosLosValores() {
        // Arrange
        Ciudad ciudad = new Ciudad("Buenos Aires", new Provincia("Buenos Aires", new Pais("Argentina")));
        Direccion direccion = new Direccion("Rivadavia", "Corrientes", 1234, 2, "A", ciudad);

        Humana humanaRep = new Humana("Maria", "Lopez", 40, 87654321, Genero.MUJER);
        Representante representante = new Representante(humanaRep, true);

        Juridica persona = new Juridica(
            direccion,
            "Fundacion Ayuda",
            "ONGs",
            TipoJuridico.ONG,
            "20-12345678-9",
            List.of(representante),
            "la mejor fundacion"
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
        Humana humana = new Humana("Juan", "Perez", 30, 12345678, Genero.HOMBRE);
        Ciudad ciudad = new Ciudad("Buenos Aires", new Provincia("Buenos Aires", new Pais("Argentina")));
        Direccion direccion = new Direccion("Rivadavia", "Corrientes", 1234, 2, "A", ciudad);

        PersonaHumana persona = new PersonaHumana(humana, direccion, "juan el mejorcito");


        assertEquals("Juan", persona.getPersona().getNombre());
        assertEquals("Perez", persona.getPersona().getApellido());
        assertEquals(30, persona.getPersona().getEdad());
        assertEquals(12345678, persona.getPersona().getNumeroDeDocumento());
        assertEquals(Genero.HOMBRE, persona.getPersona().getGenero());
        assertEquals("Juan Perez", persona.darNombre());
    }
}
