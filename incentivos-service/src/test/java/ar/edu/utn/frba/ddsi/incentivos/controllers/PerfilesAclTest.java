package ar.edu.utn.frba.ddsi.incentivos.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.utn.frba.ddsi.incentivos.config.SecurityConfig;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PerfilService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PerfilController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "seguridad.ips-internas=10.10.10.10",
        "seguridad.servicios-internos="
})
@DisplayName("ACL de perfiles")
class PerfilesAclTest {

    private static final UUID ID_USUARIO = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PerfilService perfilService;

    @Test
    @DisplayName("fuera de la allowlist responde 403")
    void fueraDeLaAllowlistEs403() throws Exception {
        mockMvc.perform(put("/api/perfiles/{id}", ID_USUARIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"Ana\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("desde una IP permitida actualiza por el flujo interno")
    void desdeIpPermitidaActualiza() throws Exception {
        when(perfilService.actualizarDatosPerfil(any(), any(), any())).thenReturn(new PerfilDTO());

        mockMvc.perform(put("/api/perfiles/{id}", ID_USUARIO)
                        .with(request -> {
                            request.setRemoteAddr("10.10.10.10");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreUsuario\":\"Ana\"}"))
                .andExpect(status().isOk());

        verify(perfilService).actualizarDatosPerfil(eq(ID_USUARIO), any(), any());
    }
}
