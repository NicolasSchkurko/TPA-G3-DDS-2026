package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MedioDeContacto")
class MedioDeContactoTest {

  @Test
  @DisplayName("equals y hashCode por tipo y valor, sin importar ID ni mayúsculas")
  void equalsPorTipoYValor() {
    Mail m1 = new Mail("usuario@test.com");
    Mail m2 = new Mail("USUARIO@TEST.COM");
    assertEquals(m1, m2);
    assertEquals(m1.hashCode(), m2.hashCode());

    Whatsapp w1 = new Whatsapp("11223344");
    Whatsapp w2 = new Whatsapp("11223344");
    assertEquals(w1, w2);
    assertEquals(w1.hashCode(), w2.hashCode());

    Telefono t1 = new Telefono("11223344");
    assertNotEquals(w1, t1, "Un Whatsapp y un Telefono con el mismo número no deben ser iguales");
  }
}
