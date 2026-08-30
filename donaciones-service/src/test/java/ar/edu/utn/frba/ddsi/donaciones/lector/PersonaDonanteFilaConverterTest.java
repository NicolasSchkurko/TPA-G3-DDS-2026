package ar.edu.utn.frba.ddsi.donaciones.lector;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter.CampoLogico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PersonaDonanteFilaConverterTest {

    private PersonaDonanteFilaConverter converter;

    @BeforeEach
    void setUp() {
        List<MapeoCSV> configuracionMapeo = List.of(
                new MapeoCSV(CampoLogico.TIPO_PERSONA.name(), List.of("TipoPersona")),
                new MapeoCSV(CampoLogico.TIPO_DOC.name(), List.of("TipoDoc")),
                new MapeoCSV(CampoLogico.DOCUMENTO.name(), List.of("Documento")),
                new MapeoCSV(CampoLogico.NOMBRE_RAZON_SOCIAL.name(), List.of("Nombre", "Apellido", "Razón Social")),
                new MapeoCSV(CampoLogico.EMAIL.name(), List.of("Email")),
                new MapeoCSV(CampoLogico.TELEFONO.name(), List.of("Teléfono"))
        );

        converter = new PersonaDonanteFilaConverter(configuracionMapeo);
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta inicializar sin mapeo de columnas")
    void constructor_LanzaExcepcionPorMapeoNuloOVacio() {
        assertThrows(IllegalArgumentException.class, () -> new PersonaDonanteFilaConverter(null));
        assertThrows(IllegalArgumentException.class, () -> new PersonaDonanteFilaConverter(new ArrayList<>()));
    }

    @Test
    @DisplayName("Debe instanciar una Humana separando nombre, apellido y parseando el DNI limpio")
    void convertir_CreaPersonaHumanaCorrectamente() {
        Map<String, String> fila = new HashMap<>();
        fila.put("TipoPersona", "HUMANA");
        fila.put("Nombre", "Juan Alberto");
        fila.put("Apellido", "Pérez");
        fila.put("Documento", "12.345.678");
        fila.put("Email", "juan@mail.com");

        Donante donante = converter.convertir(fila);

        assertNotNull(donante);
        assertNotNull(donante.getPersona());
        assertTrue(donante.getPersona() instanceof Humana);

        Humana humana = (Humana) donante.getPersona();
        assertEquals("Juan Alberto", humana.getNombre());
        assertEquals("Pérez", humana.getApellido());
        assertEquals(12345678, humana.getNumeroDeDocumento());
    }

    @Test
    @DisplayName("Debe instanciar una Juridica cuando el tipo es JURIDICA")
    void convertir_CreaPersonaJuridicaCorrectamente() {
        Map<String, String> fila = new HashMap<>();
        fila.put("TipoPersona", "JURIDICA");
        fila.put("Razón Social", "Arcos Plateados S.A.");
        fila.put("Documento", "30-12345678-9");
        fila.put("Teléfono", "+54 11 4444-4444");

        Donante donante = converter.convertir(fila);

        assertNotNull(donante);
        assertNotNull(donante.getPersona());
        assertTrue(donante.getPersona() instanceof Juridica);

        Juridica juridica = (Juridica) donante.getPersona();
        assertEquals("Arcos Plateados S.A.", juridica.getRazonSocial());
        assertEquals("30-12345678-9", juridica.getCuit());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el TipoPersona es inválido o está vacío")
    void convertir_LanzaExcepcionPorTipoPersonaDesconocido() {
        Map<String, String> fila1 = new HashMap<>();
        fila1.put("TipoPersona", "GATO");

        Map<String, String> fila2 = new HashMap<>();
        fila2.put("TipoPersona", "");

        assertThrows(IllegalArgumentException.class, () -> converter.convertir(fila1), "Debería lanzar excepción para tipo GATO");
        assertThrows(IllegalArgumentException.class, () -> converter.convertir(fila2), "Debería lanzar excepción para tipo vacío");
    }

    @Test
    @DisplayName("Debe asignar 0 al documento si contiene letras o datos no parseables, sin romper el proceso")
    void convertir_ManejaExcepcionAlParsearDocumentos() {
        Map<String, String> fila = new HashMap<>();
        fila.put("TipoPersona", "HUMANA");
        fila.put("Nombre", "Ana Gómez");
        fila.put("Documento", "SIN_DNI_VALIDO");

        Donante donante = converter.convertir(fila);

        assertNotNull(donante);
        assertNotNull(donante.getPersona());
        assertTrue(donante.getPersona() instanceof Humana);

        assertEquals(0, ((Humana) donante.getPersona()).getNumeroDeDocumento(),
                "Al fallar el parseo debe quedar en 0");
    }
}