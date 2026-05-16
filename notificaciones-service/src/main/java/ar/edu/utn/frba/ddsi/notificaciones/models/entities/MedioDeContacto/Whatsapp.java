package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

public class Whatsapp extends MedioDeContacto {
    private String numero;

    public Whatsapp(String numero) {
        this.numero = numero;
    }

    @Override
    public String getValor() {
        return numero;
    }
}

