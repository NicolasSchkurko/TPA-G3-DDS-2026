package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.services.NotificacionesDerivadasService;
import ar.edu.utn.frba.ddsi.front.services.NotificacionesUsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionesControllerTest {

    private final NotificacionesUsuarioService service = mock(NotificacionesUsuarioService.class);
    private final NotificacionesController controller = new NotificacionesController(service);

    @Test
    void detalleMarksTheUsersNotificationAsRead() {
        var unread = new NotificacionesDerivadasService.Notificacion(
                "svc-123", "servicio", "Título", "Detalle", "12 jun 2026, 10:30",
                null, false, "sistema", "Correo · ENVIADA");
        var request = new MockHttpServletRequest();
        var model = new ExtendedModelMap();
        when(service.notificaciones(request, null)).thenReturn(List.of(unread));

        String view = controller.detalle("svc-123", "noLeidas", null, request, model);

        assertEquals("detalle-notificacion", view);
        assertTrue(((NotificacionesDerivadasService.Notificacion) model.get("notificacion")).leida());
        assertEquals("noLeidas", model.get("filtro"));
        verify(service).marcarLeida(request, null, "svc-123");
    }

    @Test
    void detalleDoesNotMarkNotificationsThatDoNotBelongToTheUser() {
        var request = new MockHttpServletRequest();
        when(service.notificaciones(request, null)).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.detalle("svc-unknown", "todas", null, request, new ExtendedModelMap()));

        assertEquals(404, exception.getStatusCode().value());
        verify(service, never()).marcarLeida(request, null, "svc-unknown");
    }
}
