package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaJuridica;
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

class PersonaDonanteFilaConverterTest {

  private PersonaDonanteFilaConverter converter;

  @BeforeEach
  void setUp() {
    // Se inicializa utilizando List<MapeoCSV> en lugar de Map directo,
    // y usando los nombres del enum CampoLogico como pide el nuevo constructor.
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
  @DisplayName("Debe instanciar una PersonaHumana separando nombre, apellido y parseando el DNI limpio")
  void convertir_CreaPersonaHumanaCorrectamente() {
    // Arrange
    Map<String, String> fila = new HashMap<>();
    fila.put("TipoPersona", "HUMANA");
    fila.put("Nombre", "Juan Alberto");
    fila.put("Apellido", "Pérez");
    fila.put("Documento", "12.345.678");
    fila.put("Email", "juan@mail.com");

    // Act
    PersonaDonante donante = converter.convertir(fila);

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
  void convertir_CreaPersonaJuridicaCorrectamente() {
    // Arrange
    Map<String, String> fila = new HashMap<>();
    fila.put("TipoPersona", "JURIDICA");
    fila.put("Razón Social", "Arcos Plateados S.A.");
    fila.put("Documento", "30-12345678-9");
    fila.put("Teléfono", "+54 11 4444-4444");

    // Act
    PersonaDonante donante = converter.convertir(fila);

    // Assert
    assertNotNull(donante);
    assertTrue(donante instanceof PersonaJuridica);
    PersonaJuridica juridica = (PersonaJuridica) donante;
    assertEquals("Arcos Plateados S.A.", juridica.getRazonSocial());
    assertEquals("30-12345678-9", juridica.getCuit());
  }

@Test
@DisplayName("Debe lanzar excepción si el TipoPersona es inválido o está vacío")
void convertir_LanzaExcepcionPorTipoPersonaDesconocido() {
    // Arrange
    Map<String, String> fila1 = new HashMap<>();
    fila1.put("TipoPersona", "GATO");

    Map<String, String> fila2 = new HashMap<>();
    fila2.put("TipoPersona", "");

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> converter.convertir(fila1), "Debería lanzar excepción para tipo GATO");
    assertThrows(IllegalArgumentException.class, () -> converter.convertir(fila2), "Debería lanzar excepción para tipo vacío");
}
  @Test
  @DisplayName("Debe asignar 0 al documento si contiene letras o datos no parseables, sin romper el proceso")
  void convertir_ManejaExcepcionAlParsearDocumentos() {
    // Arrange
    Map<String, String> fila = new HashMap<>();
    fila.put("TipoPersona", "HUMANA");
    fila.put("Nombre", "Ana Gómez");
    fila.put("Documento", "SIN_DNI_VALIDO");

    // Act
    PersonaDonante donante = converter.convertir(fila);

    // Assert
    assertNotNull(donante);
    assertTrue(donante instanceof PersonaHumana);
    assertEquals(0, ((PersonaHumana) donante).getPersona().getNumeroDeDocumento(), "Al fallar el parseo debe quedar en 0");
  }

}