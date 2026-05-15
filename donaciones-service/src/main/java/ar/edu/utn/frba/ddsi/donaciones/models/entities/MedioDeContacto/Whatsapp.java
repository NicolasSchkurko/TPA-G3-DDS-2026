package ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;

public class Whatsapp extends MedioDeContacto {
    private String numeroDeTelefono;

    @Override
    public String getValor() {
        return numeroDeTelefono;
    }

    @Override
    public void enviarMensaje(String mensaje, TipoDeMensaje tipo) {
        //enviarNotificacion(tipo, "Notificación", mensaje, numeroDeTelefono);
    }
}
