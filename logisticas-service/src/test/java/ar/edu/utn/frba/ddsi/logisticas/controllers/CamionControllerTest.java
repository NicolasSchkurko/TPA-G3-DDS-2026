package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionesDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CamionController")
class CamionControllerTest {

    @Mock private CamionService camionService;

    @InjectMocks private CamionController camionController;

    private CamionDTO crearDto(String patente) {
        CamionDTO dto = new CamionDTO();
        dto.setPatente(patente);
        dto.setCapacidadVolumen(30.0);
        dto.setAltura(3.5);
        dto.setCapacidadCarga(5000.0);
        dto.setDisponible(true);
        return dto;
    }

    private MockMvc mvcConAdvice() {
        return MockMvcBuilders.standaloneSetup(new CamionController(camionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockMvc mvcConValidacion() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new CamionController(camionService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // --- CRUD ---

    @Test
    @DisplayName("POST /api/camiones con patente duplicada devuelve 409 Conflict")
    void crearCamionDuplicadoDevuelveConflict() throws Exception {
        CamionDTO dto = crearDto("E2E001");
        when(camionService.create(any(CamionDTO.class)))
                .thenThrow(new DuplicateKeyException("E11000 duplicate key error"));

        MvcResult resultado = mvcConAdvice().perform(post("/api/camiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(resultado.getResponse().getContentAsString())
                .contains("Ya existe un registro con esa clave");
    }

    @Test
    @DisplayName("POST /api/camiones exitoso devuelve 201 Created")
    void crearCamionExitosoDevuelveCreated() {
        CamionDTO dto = crearDto("ABC123");
        when(camionService.create(any(CamionDTO.class))).thenReturn(dto);

        ResponseEntity<?> response = camionController.crearCamion(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    // --- paginación ---

    @Test
    @DisplayName("GET /api/camiones sin parámetros usa page 0 y size 20")
    void sinParametrosUsaElDefault() throws Exception {
        when(camionService.findAll(any(Pageable.class)))
                .thenReturn(Page.<CamionDTO>empty(PageRequest.of(0, 20)));

        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();
        resolver.setFallbackPageable(PageRequest.of(3, 7));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CamionController(camionService))
                .setCustomArgumentResolvers(resolver)
                .build();

        mvc.perform(get("/api/camiones")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(camionService).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }

    // --- validación ---

    @Test
    @DisplayName("POST /api/camiones sin campos requeridos devuelve 400 y no llama al servicio")
    void camionIncompletoDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/camiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("patente: es obligatorio")));
    }

    @Test
    @DisplayName("POST /api/camiones/registrar sin lista devuelve 400")
    void registrarCamionesSinListaDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/camiones/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/camiones/registrar valida en cascada cada camión")
    void registrarCamionesConElementoInvalidoDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/camiones/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camiones\":[{}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/camiones/registrar con un null en la lista devuelve 400")
    void registrarCamionesConElementoNullDevuelve400() throws Exception {
        mvcConValidacion().perform(post("/api/camiones/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camiones\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/camiones con datos válidos llega al servicio")
    void camionValidoLlegaAlServicio() throws Exception {
        when(camionService.create(any(CamionDTO.class))).thenReturn(new CamionDTO());

        mvcConValidacion().perform(post("/api/camiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patente\":\"AAA111\",\"capacidadVolumen\":10.0," +
                                "\"altura\":3.0,\"capacidadCarga\":1000.0}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/camiones/registrar con lista válida llega al servicio")
    void registrarCamionesValidoLlegaAlServicio() throws Exception {
        when(camionService.createMultiple(any(CamionesDTO.class)))
                .thenReturn(new CamionesDTO(List.of()));

        mvcConValidacion().perform(post("/api/camiones/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camiones\":[{\"patente\":\"AAA111\"," +
                                "\"capacidadVolumen\":10.0,\"altura\":3.0,\"capacidadCarga\":1000.0}]}"))
                .andExpect(status().isCreated());
    }
}
