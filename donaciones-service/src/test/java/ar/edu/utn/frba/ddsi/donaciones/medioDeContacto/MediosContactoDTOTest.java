package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 57: el DTO de medios de contacto rechaza nulos, vacíos y tipos inválidos")
class MediosContactoDTOTest {

  @Test
  @DisplayName("tipo nulo no se convierte en null silencioso")
  void tipoNuloSeRechaza() {
    assertThrows(IllegalArgumentException.class,
        () -> new MediosContactoDTO(null, "a@b.com").toDomain());
  }

  @Test
  @DisplayName("valor vacío o en blanco se rechaza")
  void valorVacioSeRechaza() {
    assertThrows(IllegalArgumentException.class,
        () -> new MediosContactoDTO("EMAIL", "").toDomain());
    assertThrows(IllegalArgumentException.class,
        () -> new MediosContactoDTO("TELEFONO", "   ").toDomain());
  }

  @Test
  @DisplayName("tipo no soportado se rechaza")
  void tipoNoSoportadoSeRechaza() {
    assertThrows(IllegalArgumentException.class,
        () -> new MediosContactoDTO("SMS", "123").toDomain());
  }

  @Test
  @DisplayName("un email válido se normaliza a minúsculas")
  void emailValidoSeConvierte() {
    assertEquals("a@b.com", new MediosContactoDTO("EMAIL", " A@B.COM ").toDomain().getValor());
  }
}
