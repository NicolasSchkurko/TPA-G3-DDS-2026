package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.ALERTA;
import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.BIENVENIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MediosDeContactoTest {

    MedioDeContacto medioWhatsapp = new Whatsapp("12345", List.of(BIENVENIDA, ALERTA));
    MedioDeContacto medioWhatsapp2 = new Whatsapp("67890", List.of(BIENVENIDA, ALERTA));

    @Test
    @DisplayName("Test agregar medio de contacto")
    void agregar_medio_de_contacto() {
        MediosDeContacto medios = new MediosDeContacto();

        medios.agregarMedioDeContacto(medioWhatsapp);

        assertEquals(List.of(medioWhatsapp), medios.getListaMediosDeContacto());
    }

    @Test
    @DisplayName("Punto 57: agregar un medio nulo se rechaza y no ensucia la lista")
    void agregar_medio_nulo_se_rechaza() {
        MediosDeContacto medios = new MediosDeContacto();

        assertThrows(IllegalArgumentException.class, () -> medios.agregarMedioDeContacto(null));
        assertTrue(medios.getListaMediosDeContacto().isEmpty());
    }

    @Test
    @DisplayName("elimina por contenido y promueve el predeterminado")
    void eliminar_por_contenido_y_promueve_predeterminado() {
        MediosDeContacto coleccion = new MediosDeContacto();
        Mail mailOriginal = new Mail("contacto@donaciones.org");
        Whatsapp whatsappOriginal = new Whatsapp("1199887766");

        coleccion.agregarMedioDeContacto(mailOriginal);
        coleccion.agregarMedioDeContacto(whatsappOriginal);
        coleccion.setMedioDeContactoPredeterminado(mailOriginal);

        assertEquals(2, coleccion.getListaMediosDeContacto().size());
        assertEquals(mailOriginal, coleccion.getMedioDeContactoPredeterminado());

        coleccion.eliminarMedioDeContacto(new Mail("contacto@donaciones.org"));

        assertEquals(1, coleccion.getListaMediosDeContacto().size());
        assertTrue(coleccion.getListaMediosDeContacto().contains(whatsappOriginal));
        assertEquals(whatsappOriginal, coleccion.getMedioDeContactoPredeterminado());
    }

    @Test
    @DisplayName("Test eliminar medio de contacto")
    void eliminar_medio_de_contacto() {
        MediosDeContacto medios = new MediosDeContacto();

        medios.agregarMedioDeContacto(medioWhatsapp);

        medios.agregarMedioDeContacto(medioWhatsapp2);

        medios.eliminarMedioDeContacto(medioWhatsapp2);

        assertEquals(List.of(medioWhatsapp), medios.getListaMediosDeContacto());
    }
}