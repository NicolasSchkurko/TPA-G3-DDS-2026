package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.controllers.DonanteController;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MedioDeContactoEqualsTest {

  @Test
  @DisplayName("Punto 48: MedioDeContacto implementa equals y hashCode por tipo y valor sin importar ID ni mayúsculas")
  void medioDeContacto_equalsPorTipoYValor() {
    Mail m1 = new Mail("usuario@test.com");
    Mail m2 = new Mail("USUARIO@TEST.COM");
    assertEquals(m1, m2, "Dos Mail con el mismo email deben ser iguales independientemente de la instancia o casing");
    assertEquals(m1.hashCode(), m2.hashCode());

    Whatsapp w1 = new Whatsapp("11223344");
    Whatsapp w2 = new Whatsapp("11223344");
    assertEquals(w1, w2, "Dos Whatsapp con el mismo número deben ser iguales");
    assertEquals(w1.hashCode(), w2.hashCode());

    Telefono t1 = new Telefono("11223344");
    assertNotEquals(w1, t1, "Un Whatsapp y un Telefono con el mismo número no deben ser iguales");
  }

  @Test
  @DisplayName("Punto 48: MediosDeContacto.eliminarMedioDeContacto elimina por contenido y promueve predeterminado")
  void eliminarMedioDeContacto_eliminaInstanciaDiferenteYPromuevePredeterminado() {
    MediosDeContacto coleccion = new MediosDeContacto();
    Mail mailOriginal = new Mail("contacto@donaciones.org");
    Whatsapp whatsappOriginal = new Whatsapp("1199887766");

    coleccion.agregarMedioDeContacto(mailOriginal);
    coleccion.agregarMedioDeContacto(whatsappOriginal);
    coleccion.setMedioDeContactoPredeterminado(mailOriginal);

    assertEquals(2, coleccion.getListaMediosDeContacto().size());
    assertEquals(mailOriginal, coleccion.getMedioDeContactoPredeterminado());

    // Se solicita eliminar usando una NUEVA instancia deserializada del DTO
    Mail medioAEliminar = new Mail("contacto@donaciones.org");
    coleccion.eliminarMedioDeContacto(medioAEliminar);

    assertEquals(1, coleccion.getListaMediosDeContacto().size(), "El medio debe haberse eliminado de la lista");
    assertTrue(coleccion.getListaMediosDeContacto().contains(whatsappOriginal));
    assertEquals(whatsappOriginal, coleccion.getMedioDeContactoPredeterminado(),
        "El predeterminado debe haberse promovido a otro medio de la lista al eliminar el anterior");
  }

  @Test
  @DisplayName("Punto 48: DELETE /personas/{id}/medios-contacto responde 200 OK en vez de 302 FOUND")
  void controller_eliminarMedioContacto_responde200Ok() {
    DonanteService service = mock(DonanteService.class);
    DonanteController controller = new DonanteController(service);

    UUID id = UUID.randomUUID();
    MediosContactoDTO dto = new MediosContactoDTO("MAIL", "user@test.com");
    PersonaDonanteDTO dtoResp = new PersonaDonanteDTO();
    when(service.eliminarMedioContacto(eq(id), any(MediosContactoDTO.class))).thenReturn(dtoResp);

    ResponseEntity<?> response = controller.eliminarMedioContacto(id, dto);

    assertEquals(HttpStatus.OK, response.getStatusCode(), "El DELETE debe responder 200 OK y no 302 FOUND");
    assertSame(dtoResp, response.getBody());
  }
}
