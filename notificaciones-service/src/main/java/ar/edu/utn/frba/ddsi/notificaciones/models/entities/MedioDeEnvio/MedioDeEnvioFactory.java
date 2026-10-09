package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MedioDeEnvioFactory {

    private final Map<String, MedioDeEnvio> medios;

    @Autowired
    public MedioDeEnvioFactory(Map<String, MedioDeEnvio> medios) {
        this.medios = medios;
    }

    /** Busca en minúsculas y por alias: los servicios mandan {@code EMAIL}, {@code MAIL} o {@code WHATSAPP}. */
    public MedioDeEnvio mapearAMedioEnvio(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de medio de contacto no puede venir vacío");
        }

        String clave = tipo.trim().toLowerCase();
        MedioDeEnvio medio = medios.get(clave);

        if (medio == null) {
            medio = switch (clave) {
                case "mail", "gmail", "correo", "correoelectronico" -> medios.get("email");
                case "tel", "celular", "movil" -> medios.get("telefono");
                case "wa" -> medios.get("whatsapp");
                default -> null;
            };
        }

        if (medio == null) {
            throw new IllegalArgumentException(
                    "Tipo de medio de contacto desconocido: '" + tipo
                            + "'. Conocidos: " + medios.keySet());
        }

        return medio;
    }
}
