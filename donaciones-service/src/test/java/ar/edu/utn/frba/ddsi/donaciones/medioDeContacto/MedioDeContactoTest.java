package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.ALERTA;
import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.BIENVENIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MedioDeContactoTest {

    MedioDeContacto medioWhatsapp = new Whatsapp("12345",List.of(BIENVENIDA, ALERTA));
    String numero = "12345";
    String direccion = "donante@gmail.com";
    List<TipoDeMensaje> tiposDeMensajeAdmitidos = List.of(BIENVENIDA, ALERTA);

    @Test
    @DisplayName("Test enviar mensaje no falla si el tipo de mensaje no esta admitido")
    void test_mensajes_tipo_no_admitido (){
        assertThrows(IllegalArgumentException.class, () -> {
            medioWhatsapp.enviarMensaje(new Mensaje("Hola","Hola", TipoDeMensaje.CAMBIO_ESTADO));
        });
    }

    @Test
    @DisplayName("Test se crea mail correctamente")
    void medioDeContacto_CreaMailCorrectamente() {
        Mail mail = new Mail(direccion, tiposDeMensajeAdmitidos);

        assertEquals("donante@gmail.com", mail.getValor());

        assertEquals(BIENVENIDA, mail.getTiposDeMensajeAdmitidos().getFirst());ALERT1
    }telefonoTelefonotelefonoTelefono



    @Test
    @DisplayName("Test se crea whatsapp correctamente")
    void medioDeContacto_CreaWhatsappCorrectamente() {
        Whatsapp whatsapp = new Whatsapp(numero, tiposDeMensajeAdmitidos);

        assertEquals("12345", whatsapp.getValor(whatwhatsapp
                                                assertEquals(BIENVENIDA, mail.getTiposDeMensajeAdmitidos().getFirst());A
    }

}
