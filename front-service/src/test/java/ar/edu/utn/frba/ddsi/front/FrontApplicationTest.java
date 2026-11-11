package ar.edu.utn.frba.ddsi.front;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(FrontApplicationTest.TestBeans.class)
class FrontApplicationTest {

    private static final UUID DONATION_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IncentivosClient incentivosClient;

    @Test
    void contextLoads() {
    }

    @Test
    void registerPageRenders() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    void donationDetailIsPublicAndShowsUnassignedEntity() throws Exception {
        mockMvc.perform(get("/donaciones/{id}", DONATION_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No asignada")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Camisas")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ROPA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/detalle-donacion.css")));
    }

    @Test
    void landingRendersFeaturedDonations() throws Exception {
        when(incentivosClient.obtenerDonacionesDestacadas()).thenReturn(List.of(
                new DonacionDestacadaDTO(
                        UUID.randomUUID(), LocalDateTime.of(2026, 5, 1, 10, 0),
                        12, "ALIMENTOS", "MERCEARIA", "Comedor Los Piletones")));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Donaciones destacadas")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Comedor Los Piletones")));
    }

    @Test
    void donationListIsPublic() throws Exception {
        mockMvc.perform(get("/donaciones"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Listado de Donaciones")));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        @Primary
        DonacionesClient testDonacionesClient(RestClient restClient) {
            return new DonacionesClient(restClient, "http://localhost") {
                @Override
                public DonacionDTO obtenerDonacionPorId(UUID id) {
                    if (!DONATION_ID.equals(id)) {
                        return null;
                    }
                    DonacionDTO donacion = new DonacionDTO();
                    donacion.setId(id);
                    donacion.setDescripcion("Abrigo");
                    donacion.setDonanteName("Donante de prueba");
                    donacion.setCategoriaBienName("ROPA");
                    donacion.setSubcategoriaName("Camisas");
                    donacion.setCantidadTotalBienes(20);
                    donacion.setEstado("EN_PREPARACION");
                    return donacion;
                }

                @Override
                public java.util.List<DonacionDTO> obtenerTodasLasDonaciones() {
                    return Collections.emptyList();
                }
            };
        }
    }
}
