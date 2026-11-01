package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JuridicaTest {

    @Test
    public void crearPersonaJuridicaConTodosLosValores() {
        Humana humanaRep = new Humana("Maria", "Lopez", 40, 87654321, Genero.MUJER);
        Representante representante = new Representante(humanaRep, true);

        Juridica persona = new Juridica(
                "Fundacion Ayuda",
                "ONGs",
                TipoJuridico.ONG,
                "20-12345678-9",
                List.of(representante)
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
    public void noPermiteCrearJuridicaSinRazonSocial() {
        assertThrows(IllegalArgumentException.class, () ->
            new Juridica(null, "ONGs", TipoJuridico.ONG, "20-123", List.of()));
        assertThrows(IllegalArgumentException.class, () ->
            new Juridica("   ", "ONGs", TipoJuridico.ONG, "20-123", List.of()));
    }

    @Test
    public void crearPersonaHumanaConTodosLosValores() {
        Humana humana = new Humana("Juan", "Perez", 30, 12345678, Genero.HOMBRE);

        assertEquals("Juan", humana.getNombre());
        assertEquals("Perez", humana.getApellido());
        assertEquals(30, humana.getEdad());
        assertEquals(12345678, humana.getNumeroDeDocumento());
        assertEquals(Genero.HOMBRE, humana.getGenero());
    }

    @Test
    @DisplayName("Punto 69: anonimizar() minimiza datos corporativos y vacia representantes")
    void anonimizarJuridicaTest() {
        Humana rep = new Humana("Maria", "Lopez", 40, 87654321, Genero.MUJER);
        Representante representante = new Representante(rep, true);
        Juridica juridica = new Juridica(
                "Fundacion Ayuda",
                "ONGs",
                TipoJuridico.ONG,
                "20-12345678-9",
                new ArrayList<>(List.of(representante))
        );
        MediosDeContacto medios = new MediosDeContacto();
        medios.agregarMedioDeContacto(new Mail("info@fundacion.org"));
        juridica.setMediosDeContacto(medios);

        juridica.anonimizar();

        assertNull(juridica.getRazonSocial());
        assertNull(juridica.getRubro());
        assertNull(juridica.getTipoJuridico());
        assertNull(juridica.getCuit());
        assertTrue(juridica.getRepresentantes().isEmpty());
        assertTrue(juridica.getMediosDeContacto().getListaMediosDeContacto().isEmpty());
        assertNull(juridica.getNombreDeUsuario());
    }
}
