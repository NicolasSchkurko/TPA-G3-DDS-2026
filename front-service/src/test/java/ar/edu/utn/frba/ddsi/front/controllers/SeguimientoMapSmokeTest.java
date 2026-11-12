package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguimientoMapSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SeguimientoTokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LogisticasClient logisticasClient;

    @Test
    void elMapaUsaUnTokenYNoExponeElIdDeLaRuta() throws Exception {
        UUID idRuta = UUID.randomUUID();
        when(logisticasClient.obtenerRuta(any())).thenReturn(objectMapper.readTree(rutaJson(idRuta)));
        String token = tokenService.generar(idRuta);

        mockMvc.perform(get("/seguimiento/{token}", token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ruta en el mapa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Depósito")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(idRuta.toString()))));
    }

    @Test
    void unTokenInvalidoVuelveAlInicio() throws Exception {
        mockMvc.perform(get("/seguimiento/{token}", "token-invalido"))
                .andExpect(status().is3xxRedirection());
    }

    private static String rutaJson(UUID idRuta) {
        return """
                {
                  "idRuta": "%s",
                  "camionAsignado": {"patente": "AC482KP"},
                  "estado": "EN_CURSO",
                  "deposito": {"latitud": -34.5986317, "longitud": -58.4226973, "etiqueta": "Depósito"},
                  "paradas": [
                    {"direccion": {"calleUno": "Av. Perito Moreno", "altura": 2400, "latitud": -34.6538, "longitud": -58.4478,
                      "items": {"bienes": [{"estado": "ENTREGADA"}, {"estado": "EN_CAMINO"}]}}},
                    {"direccion": {"calleUno": "Av. La Plata", "altura": 1500, "latitud": -34.6375, "longitud": -58.3816,
                      "items": {"bienes": [{"estado": "EN_CAMINO"}]}}}
                  ]
                }
                """.formatted(idRuta);
    }
}
