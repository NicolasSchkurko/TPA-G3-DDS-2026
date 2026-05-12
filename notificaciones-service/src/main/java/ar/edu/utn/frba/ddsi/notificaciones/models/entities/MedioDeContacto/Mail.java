package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

public class Mail extends MedioDeContacto {
    private String direccion;

    public Mail(String direccion) {
        this.direccion = direccion;
    }

    @Override
    public String getValor() {
        return direccion;
    }
}
