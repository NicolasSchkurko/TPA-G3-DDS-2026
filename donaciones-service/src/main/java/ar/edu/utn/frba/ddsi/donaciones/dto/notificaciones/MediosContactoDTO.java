package ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediosContactoDTO {
    @NotBlank(message = "El tipo de medio de contacto es obligatorio")
    private String tipo;

    @NotBlank(message = "El valor del medio de contacto es obligatorio")
    private String valor;

    public MediosContactoDTO() {
    }

    public MediosContactoDTO(String tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }

    public MedioDeContacto toDomain() {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de medio de contacto es obligatorio");
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El valor del medio de contacto es obligatorio");
        }
        return switch (tipo.toUpperCase()) {
            case "EMAIL", "MAIL" -> new Mail(valor.toLowerCase());
            case "TELEFONO" -> new Telefono(valor);
            case "WHATSAPP" -> new Whatsapp(valor);
            default -> throw new IllegalArgumentException("Tipo de medio de contacto no soportado: " + tipo);
        };
    }

    public static MediosContactoDTO from(MedioDeContacto medio) {
        if (medio == null) return null;
        return new MediosContactoDTO(medio.getTipo(), medio.getValor());
    }
}