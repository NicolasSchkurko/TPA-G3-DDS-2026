package ar.edu.utn.frba.ddsi.donaciones.medioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Whatsapp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.ALERTA;
import static ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje.BIENVENIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;


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
    @DisplayName("Test eliminar medio de contacto")
    void eliminar_medio_de_contacto() {
        MediosDeContacto medios = new MediosDeContacto();

        medios.agregarMedioDeContacto(medioWhatsapp);

        medios.agregarMedioDeContacto(medioWhatsapp2);

        medios.eliminarMedioDeContacto(medioWhatsapp2);

        assertEquals(List.of(medioWhatsapp), medios.getListaMediosDeContacto());
    }
}