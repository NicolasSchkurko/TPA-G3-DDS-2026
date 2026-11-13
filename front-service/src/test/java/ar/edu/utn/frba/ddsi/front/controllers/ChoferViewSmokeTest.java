package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChoferViewSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogisticasClient logisticasClient;

    @MockBean
    private DonacionesClient donacionesClient;

    @MockBean
    private IncentivosClient incentivosClient;

    @Test
    void choferDashboardRendersForPreview() throws Exception {
        ChoferDTO chofer = chofer();
        LogisticaDTOs.Ruta activa = rutaActiva(chofer);
        LogisticaDTOs.Ruta finalizada = rutaFinalizada(activa.getCamionAsignado());

        when(logisticasClient.obtenerChoferes()).thenReturn(List.of(chofer));
        when(logisticasClient.obtenerRutaActivaDelChofer(chofer.getIdChofer())).thenReturn(activa);
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of());
        var historial = new ar.edu.utn.frba.ddsi.front.dto.PageResponse<LogisticaDTOs.Ruta>();
        historial.setContent(List.of(finalizada));
        historial.setTotalPages(2);
        historial.setTotalElements(9);
        when(logisticasClient.obtenerHistorialDelChofer(any(), anyInt(), anyInt())).thenReturn(historial);

        mockMvc.perform(get("/chofer").session(sessionChofer()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Mi ruta de hoy")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("AC482KP")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Av. Perito Moreno 2400")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Historial de rutas")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("/chofer/entregas/")));
    }

    private static ChoferDTO chofer() {
        ChoferDTO chofer = new ChoferDTO();
        chofer.setIdChofer(UUID.randomUUID());
        chofer.setNombre("Chofer DDSI");
        chofer.setDisponible(true);
        chofer.setActivo(true);
        return chofer;
    }

    private static LogisticaDTOs.Ruta rutaActiva(ChoferDTO chofer) {
        LogisticaDTOs.Camion camion = new LogisticaDTOs.Camion();
        camion.setIdChofer(chofer.getIdChofer());
        camion.setPatente("AC482KP");
        camion.setDisponible(false);

        LogisticaDTOs.Ruta ruta = new LogisticaDTOs.Ruta();
        ruta.setIdRuta(UUID.randomUUID());
        ruta.setCamionAsignado(camion);
        ruta.setFechaProgramada(LocalDate.now());
        ruta.setEstado("EN_CURSO");
        ruta.setUrlSeguimiento("/seguimiento/" + ruta.getIdRuta());
        LogisticaDTOs.Deposito deposito = new LogisticaDTOs.Deposito();
        deposito.setLatitud(-34.5986317);
        deposito.setLongitud(-58.4226973);
        deposito.setEtiqueta("Depósito");
        ruta.setDeposito(deposito);
        ruta.setParadas(List.of(
                parada("Av. Perito Moreno", 2400, "Villa Soldati", -34.6538, -58.4478,
                        bien("PENDIENTE", 1600, "KG"),
                        bien("ENTREGADA", 80, "UNIDADES")),
                parada("Av. La Plata", 1500, "Barracas", -34.6375, -58.3816,
                        bien("EN_CAMINO", 300, "UNIDADES"))));
        return ruta;
    }

    private static LogisticaDTOs.Ruta rutaFinalizada(LogisticaDTOs.Camion camion) {
        LogisticaDTOs.Ruta ruta = new LogisticaDTOs.Ruta();
        ruta.setIdRuta(UUID.randomUUID());
        ruta.setCamionAsignado(camion);
        ruta.setFechaProgramada(LocalDate.now().minusDays(2));
        ruta.setEstado("FINALIZADA");
        ruta.setParadas(List.of());
        return ruta;
    }

    private static LogisticaDTOs.Parada parada(String calle, int altura, String ciudad,
                                              double lat, double lng, LogisticaDTOs.Bien... bienes) {
        LogisticaDTOs.DireccionLogistica direccion = new LogisticaDTOs.DireccionLogistica();
        direccion.setCalleUno(calle);
        direccion.setAltura(altura);
        direccion.setCiudad(ciudad);
        direccion.setLatitud(lat);
        direccion.setLongitud(lng);

        LogisticaDTOs.Bienes items = new LogisticaDTOs.Bienes();
        items.setBienes(List.of(bienes));
        items.setIdsDonaciones(java.util.Arrays.stream(bienes).map(b -> java.util.UUID.randomUUID()).toList());

        LogisticaDTOs.Parada parada = new LogisticaDTOs.Parada();
        parada.setDireccion(direccion);
        parada.setItems(items);
        return parada;
    }

    private static LogisticaDTOs.Bien bien(String estado, int cantidad, String unidad) {
        LogisticaDTOs.Bien bien = new LogisticaDTOs.Bien();
        bien.setEstado(estado);
        bien.setCantidad(cantidad);
        bien.setUnidadDeMedida(unidad);
        return bien;
    }

    private static MockHttpSession sessionChofer() {
        OidcUser user = new OidcUser() {
            @Override public Map<String, Object> getClaims() { return Map.of("email", "chofer@ddsi.local"); }
            @Override public OidcUserInfo getUserInfo() { return null; }
            @Override public OidcIdToken getIdToken() { return null; }
            @Override public Map<String, Object> getAttributes() { return getClaims(); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_CHOFER"));
            }
            @Override public String getName() { return "chofer@ddsi.local"; }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "password", List.of(new SimpleGrantedAuthority("ROLE_CHOFER"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
