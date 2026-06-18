package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class MediosContactoDTO {
    private String tipo; // "EMAIL", "TELEFONO", "WHATSAPP", etc.
    private String valor; // El correo electrónico o número telefónico

    public MediosContactoDTO(String tipo, String valor) {
        this.tipo = tipo;
        this.valor = valor;
    }
}

