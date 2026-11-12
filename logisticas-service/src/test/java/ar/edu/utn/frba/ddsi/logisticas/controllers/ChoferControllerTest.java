package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.ChoferService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChoferController")
class ChoferControllerTest {

    @Mock private ChoferService choferService;

    private MockMvc mvcConValidacion() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new ChoferController(choferService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("POST /api/choferes sin nombre devuelve 400 y no llama al servicio")
    void choferIncompletoDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/choferes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(choferService);
    }

    @Test
    @DisplayName("POST /api/choferes/registrar sin lista devuelve 400")
    void registrarChoferesSinListaDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/choferes/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(choferService);
    }

    @Test
    @DisplayName("POST /api/choferes/registrar valida en cascada cada chofer")
    void registrarChoferesConElementoInvalidoDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/choferes/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choferes\":[{}]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(choferService);
    }

    @Test
    @DisplayName("POST /api/choferes con nombre llega al servicio")
    void choferValidoLlegaAlServicio() throws Exception {
        when(choferService.create(any(ChoferDTO.class))).thenReturn(new ChoferDTO());

        mvcConValidacion().perform(post("/api/choferes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\"}"))
                .andExpect(status().isCreated());
    }
}
