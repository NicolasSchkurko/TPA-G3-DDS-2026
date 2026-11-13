package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.donaciones.config.SecurityConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DonanteController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "seguridad.ips-internas=10.10.10.10",
        "seguridad.servicios-internos="
})
@DisplayName("ACL del contacto interno de donantes")
class ContactoAclTest {

    private static final UUID ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonanteService donanteService;

    @Test
    @DisplayName("fuera de la allowlist responde 403")
    void fueraDeLaAllowlistEs403() throws Exception {
        mockMvc.perform(get("/api/personas/{id}/medios-contacto", ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("desde una IP permitida devuelve los medios de contacto")
    void desdeIpPermitidaResponde() throws Exception {
        when(donanteService.obtenerMediosContacto(ID)).thenReturn(List.<MediosContactoDTO>of());

        mockMvc.perform(get("/api/personas/{id}/medios-contacto", ID)
                        .with(request -> {
                            request.setRemoteAddr("10.10.10.10");
                            return request;
                        }))
                .andExpect(status().isOk());
    }
}
