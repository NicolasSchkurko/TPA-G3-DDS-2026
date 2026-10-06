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

    /**
     * Resuelve el medio de envío a partir del tipo que manda el servicio de dominio.
     *
     * <p><b>Busca en minúsculas y por alias, y el motivo es concreto.</b> El mapa de Spring
     * viene indexado por nombre de bean, que es {@code "email"}, {@code "telefono"} y
     * {@code "whatsapp"} en minúscula, y el lookup anterior era case-sensitive. El resultado
     * era que cualquier tipo en mayúsculas —que es exactamente como lo mandan los otros
     * servicios: {@code Mail.java} devuelve {@code "EMAIL"} y {@code NotificacionEntregaFallida}
     * devuelve {@code "WHATSAPP"}— caía en el {@code null} y terminaba en
     * {@code "Tipo desconocido"}. La notificación quedaba en estado FALLIDA y el flujo de
     * incentivización entero, porque nunca se encontraba el medio.
     *
     * <p>Los alias cubren las dos grafías que usan los servicios: el nombre del bean
     * ({@code email}) y el tipo declarado por el dominio ({@code MAIL}, {@code GMAIL}).
     */
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

