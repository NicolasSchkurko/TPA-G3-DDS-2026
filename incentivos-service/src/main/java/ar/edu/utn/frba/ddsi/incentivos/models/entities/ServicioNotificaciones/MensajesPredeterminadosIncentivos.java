package ar.edu.utn.frba.ddsi.incentivos.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.TipoDeMensaje;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.springframework.stereotype.Component;

/**
 * Permite crear mensajes predeterminados según el evento por el
 * que se está notificando
 */

@Component
public class MensajesPredeterminadosIncentivos {

  public Mensaje mensajeMisionCumplida(Perfil perfil) {
    String asunto = "Mision Completa";
    String cuerpo = "Felicitaciones "
            +perfil.getNombreUsuario()
            +", has conseguido una nueva Insignia: "
            + perfil.getInsignias().getLast().getNombre() + "/n"
            + perfil.getInsignias().getLast().getDescripcion() + "/n"
            + perfil.getInsignias().getLast().getUrlImagen();

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.MISION);
  }

  public Mensaje mensajeCambioCategoria(Perfil perfil, Perfil perfilAnterior) {
    String asunto = "Ascenso Categoria";
    String cuerpo = "Felicitaciones "
            +perfil.getNombreUsuario()
            +", has ascendido de "
            +perfilAnterior.getCategoriaActual().name()
            + " a la nueva categoria "
            +perfil.getCategoriaActual().name();

    return new Mensaje(asunto, cuerpo, TipoDeMensaje.RECOMPENSAS);
  }

  private String valorOTexto(String valor, String textoPorDefecto) {
    if (valor == null || valor.isBlank()) {
      return textoPorDefecto;
    }
    return valor;
  }
}
