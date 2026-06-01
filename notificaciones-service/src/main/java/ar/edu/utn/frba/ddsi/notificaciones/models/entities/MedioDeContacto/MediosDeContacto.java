package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeContacto;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;

import java.util.ArrayList;
import java.util.List;

public class MediosDeContacto {
    private MedioDeContacto medioDeContactoPredeterminado;

    // Se inicializa la lista para evitar el NullPointerException
    private List<MedioDeContacto> listaMediosDeContacto = new ArrayList<>();

    public MediosDeContacto() {}

    public void agregarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.listaMediosDeContacto.add(medioDeContacto);
    }

    public void agregarMediosDeContacto(List<MedioDeContacto> mediosDeContacto) {
        this.listaMediosDeContacto.addAll(mediosDeContacto);
    }

    public void eliminarMedioDeContacto(MedioDeContacto medioDeContacto) {
        this.listaMediosDeContacto.remove(medioDeContacto);
    }

    public void eliminarMediosDeContacto(List<MedioDeContacto> mediosDeContacto) {
        this.listaMediosDeContacto.removeAll(mediosDeContacto);
    }

    @Override
    public String toString() {
        return "MediosDeContacto{listaMediosDeContacto=" + listaMediosDeContacto + '}';
    }
    public void enviarNotificacionAMedios(Notificacion notificacion) {
        listaMediosDeContacto.forEach(medio -> medio.enviarNotificacion(notificacion));
    }
}
