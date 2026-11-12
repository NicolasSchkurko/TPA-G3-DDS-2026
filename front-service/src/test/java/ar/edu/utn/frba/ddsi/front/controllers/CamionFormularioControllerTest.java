package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.front.services.AdminResumenService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CamionFormularioControllerTest {

    @Test
    void altaGetRenderiza() {
        LogisticasClient logisticasClient = mock(LogisticasClient.class);
        when(logisticasClient.obtenerChoferes()).thenReturn(List.of());
        AdminController controller = new AdminController(logisticasClient, mock(DonacionesClient.class),
                mock(AdminResumenService.class), mock(SeguimientoTokenService.class));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.nuevoCamion(model);

        assertEquals("admin/camion-formulario", view);
        assertNotNull(model.getAttribute("camion"));
    }

    @Test
    void altaPostRedirigeYLLamaCliente() {
        LogisticasClient logisticasClient = mock(LogisticasClient.class);
        AdminController controller = new AdminController(logisticasClient, mock(DonacionesClient.class),
                mock(AdminResumenService.class), mock(SeguimientoTokenService.class));
        CamionDTO camion = new CamionDTO();
        camion.setPatente("ABC123");

        String view = controller.crearCamion(camion, new RedirectAttributesModelMap());

        assertEquals("redirect:/admin/camiones", view);
        verify(logisticasClient).crearCamion(any(CamionDTO.class));
    }

    @Test
    void edicionGetRenderiza() {
        LogisticasClient logisticasClient = mock(LogisticasClient.class);
        CamionDTO camion = new CamionDTO();
        camion.setPatente("ABC123");
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of(camion));
        when(logisticasClient.obtenerChoferes()).thenReturn(List.of());
        AdminController controller = new AdminController(logisticasClient, mock(DonacionesClient.class),
                mock(AdminResumenService.class), mock(SeguimientoTokenService.class));
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.editarCamion("ABC123", model, new RedirectAttributesModelMap());

        assertEquals("admin/camion-formulario", view);
        assertNotNull(model.getAttribute("camion"));
    }

    @Test
    void edicionPostRedirigeYLLamaCliente() {
        LogisticasClient logisticasClient = mock(LogisticasClient.class);
        AdminController controller = new AdminController(logisticasClient, mock(DonacionesClient.class),
                mock(AdminResumenService.class), mock(SeguimientoTokenService.class));

        String view = controller.actualizarCamion("ABC123", new CamionDTO(), new RedirectAttributesModelMap());

        assertEquals("redirect:/admin/camiones", view);
        verify(logisticasClient).actualizarCamion(org.mockito.ArgumentMatchers.eq("ABC123"), any(CamionDTO.class));
    }
}
