package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.ALERTA;
import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.BIENVENIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class MedioDeContactoTest {

    MedioDeContacto medioWhatsapp = new Whatsapp("12345", List.of(BIENVENIDA, ALERTA));
    String numero = "12345";
    String direccion = "donante@gmail.com";
    List<TipoDeMensaje> tiposDeMensajeProhibidos = List.of(BIENVENIDA, ALERTA);

    @Test
    @DisplayName("Test enviar mensaje falla si el tipo de mensaje esta prohibido")
    void test_mensajes_tipo_prohibido() {
        assertThrows(IllegalArgumentException.class, () -> {
            medioWhatsapp.enviarMensaje(new Mensaje("Hola", "Hola", BIENVENIDA));
        });
    }

    @Test
    @DisplayName("Test medio de contacto se inicializa sin tipos prohibidos")
    void medioDeContacto_SeInicializaSinTiposProhibidos() {
        Mail mail = new Mail(direccion);

        assertEquals(List.of(), mail.getTiposDeMensajeProhibidos());
    }

    @Test
    @DisplayName("Test se crea mail correctamente")
    void medioDeContacto_CreaMailCorrectamente() {
        Mail mail = new Mail(direccion, tiposDeMensajeProhibidos);

        assertEquals("donante@gmail.com", mail.getValor());
        assertEquals(BIENVENIDA, mail.getTiposDeMensajeProhibidos().get(0));
        assertEquals(ALERTA, mail.getTiposDeMensajeProhibidos().get(1));
    }

    @Test
    @DisplayName("Test se crea telefono correctamente")
    void medioDeContacto_CreaTelefonoCorrectamente() {
        Telefono telefono = new Telefono(numero, tiposDeMensajeProhibidos);

        assertEquals("12345", telefono.getValor());
        assertEquals(BIENVENIDA, telefono.getTiposDeMensajeProhibidos().get(0));
        assertEquals(ALERTA, telefono.getTiposDeMensajeProhibidos().get(1));
    }

    @Test
    @DisplayName("Test se crea whatsapp correctamente")
    void medioDeContacto_CreaWhatsappCorrectamente() {
        Whatsapp whatsapp = new Whatsapp(numero, tiposDeMensajeProhibidos);

        assertEquals("12345", whatsapp.getValor());
        assertEquals(BIENVENIDA, whatsapp.getTiposDeMensajeProhibidos().get(0));
        assertEquals(ALERTA, whatsapp.getTiposDeMensajeProhibidos().get(1));
    }

}
