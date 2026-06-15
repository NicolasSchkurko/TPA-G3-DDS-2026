package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import ar.edu.utn.frba.ddsi.notificaciones.config.mailClient.MailClient;
import ar.edu.utn.frba.ddsi.notificaciones.config.telefonoClient.TelefonoClient;
import ar.edu.utn.frba.ddsi.notificaciones.config.whatsappClient.WhatsappClient;
import ar.edu.utn.frba.ddsi.notificaciones.exceptions.NotificacionExceptions.TipoMedioDeContactoInvalidoException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Crea el MedioDeEnvio correspondiente a partir del tipo solicitado.
 * Centraliza la seleccion del medio para que el gestor no conozca los clients de cada canal.
 * Aquí se puede cambiar que client usa cada medio
 */
@Component
public class MedioDeEnvioFactory {
    private final MailClient mailClient;
    private final TelefonoClient telefonoClient;
    private final WhatsappClient whatsappClient;

    public MedioDeEnvioFactory(MailClient mailClient, TelefonoClient telefonoClient, WhatsappClient whatsappClient) {
        this.mailClient = mailClient;
        this.telefonoClient = telefonoClient;
        this.whatsappClient = whatsappClient;
    }

    public MedioDeEnvio crear(String tipoMedioContacto) {
        String tipoNormalizado = tipoMedioContacto.toLowerCase(Locale.ROOT).trim();

        return switch (tipoNormalizado) {
            case "whatsapp" -> new Whatsapp(whatsappClient);
            case "telefono", "sms" -> new Telefono(telefonoClient);
            case "mail", "email" -> new Mail(mailClient);
            default -> throw new TipoMedioDeContactoInvalidoException(
                    "El medio de contacto no es valido: " + tipoMedioContacto
            );
        };
    }
}
