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
    List<TipoDeMensaje> tiposDeMensajeAdmitidos = List.of(BIENVENIDA, ALERTA);

    @Test
    @DisplayName("Test enviar mensaje no falla si el tipo de mensaje no esta admitido")
    void test_mensajes_tipo_no_admitido() {
        assertThrows(IllegalArgumentException.class, () -> {
            medioWhatsapp.enviarMensaje(new Mensaje("Hola", "Hola", TipoDeMensaje.CAMBIO_ESTADO));
        });
    }

    @Test
    @DisplayName("Test se crea mail correctamente")
    void medioDeContacto_CreaMailCorrectamente() {
        Mail mail = new Mail(direccion, tiposDeMensajeAdmitidos);

        assertEquals("donante@gmail.com", mail.getValor());
        assertEquals(BIENVENIDA, mail.getTiposDeMensajeAdmitidos().get(0));
        assertEquals(ALERTA, mail.getTiposDeMensajeAdmitidos().get(1));
    }

    @Test
    @DisplayName("Test se crea telefono correctamente")
    void medioDeContacto_CreaTelefonoCorrectamente() {
        Telefono telefono = new Telefono(numero, tiposDeMensajeAdmitidos);

        assertEquals("12345", telefono.getValor());
        assertEquals(BIENVENIDA, telefono.getTiposDeMensajeAdmitidos().get(0));
        assertEquals(ALERTA, telefono.getTiposDeMensajeAdmitidos().get(1));
    }

    @Test
    @DisplayName("Test se crea whatsapp correctamente")
    void medioDeContacto_CreaWhatsappCorrectamente() {
        Whatsapp whatsapp = new Whatsapp(numero, tiposDeMensajeAdmitidos);

        assertEquals("12345", whatsapp.getValor());
        assertEquals(BIENVENIDA, whatsapp.getTiposDeMensajeAdmitidos().get(0));
        assertEquals(ALERTA, whatsapp.getTiposDeMensajeAdmitidos().get(1));
    }

}
