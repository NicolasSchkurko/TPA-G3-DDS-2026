package ar.edu.utn.frba.ddsi.logisticas.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.logisticas.config.SecurityConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CamionController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "seguridad.ips-internas=10.10.10.10",
        "seguridad.servicios-internos="
})
@DisplayName("ACL de la gestión de camiones")
class CamionAclTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CamionService camionService;

    @Test
    @DisplayName("fuera de la allowlist responde 403")
    void fueraDeLaAllowlistEs403() throws Exception {
        mockMvc.perform(get("/api/camiones"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("desde una IP permitida el listado responde")
    void listadoDesdeIpPermitida() throws Exception {
        when(camionService.findAll(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/camiones")
                        .with(request -> {
                            request.setRemoteAddr("10.10.10.10");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("desde una IP permitida se puede crear un camión")
    void crearDesdeIpPermitida() throws Exception {
        when(camionService.create(any())).thenReturn(new CamionDTO());

        mockMvc.perform(post("/api/camiones")
                        .with(request -> {
                            request.setRemoteAddr("10.10.10.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(camionJson()))
                .andExpect(status().isCreated());
    }

    private static String camionJson() {
        return """
                {"patente":"ABC123","capacidadVolumen":10.0,"altura":2.5,"capacidadCarga":100.0}
                """;
    }
}
