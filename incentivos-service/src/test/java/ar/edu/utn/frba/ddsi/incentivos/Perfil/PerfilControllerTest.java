package ar.edu.utn.frba.ddsi.incentivos.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PerfilControllerTest {
    @Test
    public void crearPerfil() throws Exception {
        CrearPerfilDTO dto = new CrearPerfilDTO(
                UUID.randomUUID(),
                "sofia"
        );

        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }
}
