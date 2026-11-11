package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.NotificacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.TipoDeMensaje;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioNotificacionesTest {

    @Test
    void elDtoPublicadoIncluyeElUrlMapa() {
        NotificacionesClient client = mock(NotificacionesClient.class);
        ServicioNotificaciones servicio = new ServicioNotificaciones(client);
        MedioDeContacto medio = mock(MedioDeContacto.class);
        when(medio.getTipo()).thenReturn("EMAIL");
        when(medio.getValor()).thenReturn("ana@test.com");
        Mensaje mensaje = new Mensaje("Ruta", "En viaje", TipoDeMensaje.CAMBIO_ESTADO);
        mensaje.setUrlMapa("https://mapa/1");

        servicio.enviarNotificacion(medio, mensaje);

        ArgumentCaptor<NotificacionDTO> captor = ArgumentCaptor.forClass(NotificacionDTO.class);
        verify(client).enviarNotificacion(captor.capture());
        assertEquals("https://mapa/1", captor.getValue().getUrlMapa());
        assertEquals("email", captor.getValue().getMedioDeContacto());
    }
}
