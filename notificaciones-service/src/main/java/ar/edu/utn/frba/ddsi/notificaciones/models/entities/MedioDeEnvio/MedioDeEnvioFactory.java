package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.config.mailClient.MailClient;
import ar.edu.utn.frba.ddsi.notificaciones.config.telefonoClient.TelefonoClient;
import ar.edu.utn.frba.ddsi.notificaciones.config.whatsappClient.WhatsappClient;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.TipoMedioDeContactoInvalidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Crea el MedioDeEnvio correspondiente a partir del tipo solicitado.
 * Centraliza la seleccion del medio para que el gestor no conozca los clients de cada canal.
 * Aquí se puede cambiar que client usa cada medio
 */
@Component
public class MedioDeEnvioFactory {
    private Map<String, MedioDeEnvio> medios;

    @Autowired
    public MedioDeEnvioFactory(Map<String, MedioDeEnvio> medios) {
        this.medios = medios;
    }

    public MedioDeEnvio mapearAMedioEnvio(String tipo) {
        MedioDeEnvio medio = medios.get(tipo);
        if (medio == null) {
            throw new IllegalArgumentException("Tipo desconocido: " + tipo);
        }
        return medio;
    }
}

