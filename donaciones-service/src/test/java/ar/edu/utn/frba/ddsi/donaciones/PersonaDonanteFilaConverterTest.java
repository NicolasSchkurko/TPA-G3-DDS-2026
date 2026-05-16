package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter.PersonaDonanteFilaConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PersonaDonanteFilaConverterTest {

  private PersonaDonanteFilaConverter converter;

  @BeforeEach
  void setUp() {
    Map<String, List<String>> configuracionMapeo = new HashMap<>();
    configuracionMapeo.put(PersonaDonanteFilaConverter.CAMPO_TIPO_PERSONA, List.of("TipoPersona"));
    configuracionMapeo.put(PersonaDonanteFilaConverter.CAMPO_TIPO_DOC, List.of("TipoDoc"));
    configuracionMapeo.put(PersonaDonanteFilaConverter.CAMPO_DOCUMENTO, List.of("Documento"));
    configuracionMapeo.put(PersonaDonanteFilaConverter.CAMPO_NOMBRE_RAZON, List.of("Nombre", "Apellido", "Razón Social"));
    configuracionMapeo.put(PersonaDonanteFilaConverter.CAMPO_EMAIL, List.of("Email"));
    configuracionMapeo.put(PersonaDonanteFilaConverter.CAMPO_TELEFONO, List.of("Teléfono"));

    converter = new PersonaDonanteFilaConverter(configuracionMapeo);
  }

  @Test
  @DisplayName("Debe lanzar excepción si se intenta inicializar sin mapeo de columnas")
  void constructor_LanzaExcepcionPorMapeoNuloOVacio() {
    assertThrows(IllegalArgumentException.class, () -> new PersonaDonanteFilaConverter(null));
    assertThrows(IllegalArgumentException.class, () -> new PersonaDonanteFilaConverter(new HashMap<>()));
  }

  @Test
  @DisplayName("Debe instanciar una PersonaHumana separando nombre, apellido y parseando el DNI limpio")
  void convert_CreaPersonaHumanaCorrectamente() {
    // Arrange
    Map<String, String> fila = new HashMap<>();
    fila.put("TipoPersona", "HUMANA");
    fila.put("Nombre", "Juan Alberto");
    fila.put("Apellido", "Pérez");
    fila.put("Documento", "12.345.678");
    fila.put("Email", "juan@mail.com");

    // Act
    PersonaDonante donante = converter.convert(fila);

    // Assert
    assertNotNull(donante);
    assertTrue(donante instanceof PersonaHumana);

    PersonaHumana humana = (PersonaHumana) donante;
    assertNotNull(humana.getPersona());
    assertEquals("Juan Alberto", humana.getPersona().getNombre());
    assertEquals("Pérez", humana.getPersona().getApellido());
    assertEquals(12345678, humana.getPersona().getNumeroDeDocumento());
  }

  @Test
  @DisplayName("Debe instanciar una PersonaJuridica cuando el tipo es JURIDICA")
  void convert_CreaPersonaJuridicaCorrectamente() {
    // Arrange
    Map<String, String> fila = new HashMap<>();
    fila.put("TipoPersona", "JURIDICA");
    fila.put("Razón Social", "Arcos Plateados S.A.");
    fila.put("Documento", "30-12345678-9");
    fila.put("Teléfono", "+54 11 4444-4444");

    // Act
    PersonaDonante donante = converter.convert(fila);

    // Assert
    assertNotNull(donante);
    assertTrue(donante instanceof PersonaJuridica);
    PersonaJuridica juridica = (PersonaJuridica) donante;
    assertEquals("Arcos Plateados S.A.", juridica.getRazonSocial());
    assertEquals("30-12345678-9", juridica.getCuit());
  }

  @Test
  @DisplayName("Debe retornar null e ignorar la fila si el TipoPersona es inválido o está vacío")
  void convert_RetornaNullPorTipoPersonaDesconocido() {
    // Arrange
    Map<String, String> fila1 = new HashMap<>();
    fila1.put("TipoPersona", "GATO");

    Map<String, String> fila2 = new HashMap<>();
    fila2.put("TipoPersona", "");

    // Act
    PersonaDonante resultado1 = converter.convert(fila1);
    PersonaDonante resultado2 = converter.convert(fila2);

    // Assert
    assertNull(resultado1, "Debería retornar null para tipo GATO");
    assertNull(resultado2, "Debería retornar null para tipo vacío");
  }

  @Test
  @DisplayName("Debe asignar 0 al documento si contiene letras o datos no parseables, sin romper el proceso")
  void convert_ManejaExcepcionAlParsearDocumentos() {
    // Arrange
    Map<String, String> fila = new HashMap<>();
    fila.put("TipoPersona", "HUMANA");
    fila.put("Nombre", "Ana Gómez");
    fila.put("Documento", "SIN_DNI_VALIDO");

    // Act
    PersonaDonante donante = converter.convert(fila);

    // Assert
    assertNotNull(donante);
    assertTrue(donante instanceof PersonaHumana);
    assertEquals(0, ((PersonaHumana) donante).getPersona().getNumeroDeDocumento(), "Al fallar el parseo debe quedar en 0");
  }

}