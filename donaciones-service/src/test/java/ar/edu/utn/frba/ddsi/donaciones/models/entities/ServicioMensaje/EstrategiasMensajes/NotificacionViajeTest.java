package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.EstrategiasMensajes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.DatosMensajes.NotificacionViajeDatos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.ServicioNotificaciones;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificacionViajeTest {

    @Test
    void elMapaViajaComoCampoEstructuradoYEnElTexto() {
        ServicioNotificaciones servicio = mock(ServicioNotificaciones.class);
        MediosDeContacto contactos = mock(MediosDeContacto.class);
        NotificacionViaje estrategia = new NotificacionViaje(servicio);

        estrategia.ejecutar(new NotificacionViajeDatos(
                "https://mapa/1", contactos, contactos));

        ArgumentCaptor<Mensaje> captor = ArgumentCaptor.forClass(Mensaje.class);
        verify(servicio, times(2)).enviarNotificacionAMediosDeContacto(any(), captor.capture());

        Mensaje mensaje = captor.getValue();
        assertEquals("https://mapa/1", mensaje.getUrlMapa());
        assertTrue(mensaje.getCuerpo().contains("https://mapa/1"));
    }
}
