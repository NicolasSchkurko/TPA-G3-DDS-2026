package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.donaciones.config.SecurityConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.admin.AdminDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "seguridad.ips-internas=10.10.10.10",
        "seguridad.servicios-internos="
})
@DisplayName("ACL del alta de administradores")
class AdminAclTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Test
    @DisplayName("fuera de la allowlist responde 403")
    void fueraDeLaAllowlistEs403() throws Exception {
        mockMvc.perform(post("/api/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("desde una IP permitida crea el administrador")
    void desdeIpPermitidaCrea() throws Exception {
        when(adminService.crearAdministrador(any())).thenReturn(new AdminDTO());

        mockMvc.perform(post("/api/admins")
                        .with(request -> {
                            request.setRemoteAddr("10.10.10.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }
}
