package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

public class Telefono extends MedioDeContacto {
    private String numero;

    public Telefono(String numero) {
        this.numero = numero;
    }

    @Override
    public String getValor() {
        return numero;
    }
}

