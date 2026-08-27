package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JuridicaTest {

    @Test
    public void crearPersonaJuridicaConTodosLosValores() {
        Humana humanaRep = new Humana("Maria", "Lopez", 40, 87654321, Genero.MUJER, "maria");
        Representante representante = new Representante(humanaRep, true);

        Juridica persona = new Juridica(
                "Fundacion Ayuda",
                "ONGs",
                TipoJuridico.ONG,
                "20-12345678-9",
                List.of(representante),
                "la mejor fundacion"
        );

        assertEquals("Fundacion Ayuda", persona.getRazonSocial());
        assertEquals("ONGs", persona.getRubro());
        assertEquals(TipoJuridico.ONG, persona.getTipoJuridico());
        assertEquals("20-12345678-9", persona.getCuit());
        assertEquals(1, persona.getRepresentantes().size());
        assertTrue(persona.getRepresentantes().contains(representante));
        assertEquals("Fundacion Ayuda", persona.getNombreDeUsuario());
    }

    @Test
    public void crearPersonaHumanaConTodosLosValores() {
        Humana humana = new Humana("Juan", "Perez", 30, 12345678, Genero.HOMBRE, "juan");

        assertEquals("Juan", humana.getNombre());
        assertEquals("Perez", humana.getApellido());
        assertEquals(30, humana.getEdad());
        assertEquals(12345678, humana.getNumeroDeDocumento());
        assertEquals(Genero.HOMBRE, humana.getGenero());
        assertEquals("juan", humana.getNombreDeUsuario());
    }
}