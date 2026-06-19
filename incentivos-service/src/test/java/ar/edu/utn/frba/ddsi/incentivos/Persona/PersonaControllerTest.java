package ar.edu.utn.frba.ddsi.incentivos.Persona;

import ar.edu.utn.frba.ddsi.incentivos.controllers.PersonaController;
import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.services.PersonaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PersonaControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PersonaService personaService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new PersonaController(personaService)).build();
    }

    @Test
    public void crearPerfil_created() throws Exception {
        UUID id = UUID.randomUUID();
        PerfilDonanteDTO dto = new PerfilDonanteDTO(id, "usuarioTest");
        PerfilDTO retorno = new PerfilDTO("usuarioTest", "categoria", List.of("ins1"), "mision", 1);

        when(personaService.crearPerfil(any())).thenReturn(retorno);

        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post("/perfiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    public void actualizarPerfil_created() throws Exception {
        UUID id = UUID.randomUUID();
        ImpactoDonacionDTO impacto = new ImpactoDonacionDTO();
        impacto.setIdUsuario(id);
        impacto.setFechaEntrega(LocalDate.now());
        impacto.setCantidadBienes(2);
        impacto.setCategoria("categoria");
        impacto.setSubCategoria("sub");
        impacto.setEntidadBeneficiaria("entidad");
        impacto.setEstado("ENTREGADA");

        PerfilDTO retorno = new PerfilDTO("usuarioTest", "categoria", List.of(), "mision", 2);

        when(personaService.actualizarPerfil(any())).thenReturn(retorno);

        String json = objectMapper.writeValueAsString(impacto);

        mockMvc.perform(post("/perfiles/{id}/perfil", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }
}
