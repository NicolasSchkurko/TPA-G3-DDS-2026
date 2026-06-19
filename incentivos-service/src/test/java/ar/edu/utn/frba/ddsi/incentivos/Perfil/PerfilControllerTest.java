package ar.edu.utn.frba.ddsi.incentivos.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
public class PerfilControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void crearPerfil() throws Exception {
        PerfilDonanteDTO dto = new PerfilDonanteDTO(
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
