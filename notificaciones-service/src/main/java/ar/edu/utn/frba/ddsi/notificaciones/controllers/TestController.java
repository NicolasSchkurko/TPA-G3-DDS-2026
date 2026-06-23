package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvio;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio.MedioDeEnvioFactory;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    private final MedioDeEnvioFactory factory;

    public TestController(MedioDeEnvioFactory factory) {
        this.factory = factory;
    }

    @GetMapping("/email")
    public String enviarTest() throws Exception {

        Notificacion n = new Notificacion(
                "nahuelmarek@hotmail.com",
                new Mensaje("Prueba desde Java", "Funciona con n8n 🚀")
        );

        MedioDeEnvio medio = factory.mapearAMedioEnvio("email");
        medio.enviarNotificacion(n);

        return "Enviado";
    }
}

