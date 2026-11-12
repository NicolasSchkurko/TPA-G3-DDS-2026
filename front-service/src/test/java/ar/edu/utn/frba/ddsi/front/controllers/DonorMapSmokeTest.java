package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.services.IdentidadService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoService;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DonorMapSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private DonacionesClient donacionesClient;
    @MockBean private IncentivosClient incentivosClient;
    @MockBean private IdentidadService identidadService;
    @MockBean private SeguimientoService seguimientoService;

    @Test
    void donorMapRendersWithTruckAndDepot() throws Exception {
        UUID idDonante = UUID.randomUUID();
        when(identidadService.donanteIdActual(any())).thenReturn(Optional.of(idDonante));
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of(entidad()));
        when(donacionesClient.obtenerDonacionesDeDonante(idDonante)).thenReturn(List.of(donacion()));
        when(seguimientoService.construirDonante(any())).thenReturn(seguimiento());
        when(incentivosClient.obtenerPerfilPublico(idDonante.toString()))
                .thenReturn(Map.of("nombreCategoria", "Colaborador solidario"));

        mockMvc.perform(get("/donante/mapa").session(sessionDonante()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("DONANTE_SEGUIMIENTO")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Comedor Los Piletones")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Categoría actual")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("Colaborador solidario")));
    }

    private static EntidadBeneficiariaDTO entidad() {
        EntidadBeneficiariaDTO entidad = new EntidadBeneficiariaDTO();
        entidad.setId(UUID.randomUUID());
        entidad.setRazonSocial("Comedor Los Piletones");
        entidad.setRubro("Asistencia social");
        DireccionDTO direccion = new DireccionDTO();
        direccion.setCalleUno("Av. Perito Moreno");
        direccion.setAltura(2400);
        direccion.setCiudad("Villa Soldati");
        direccion.setLatitud(-34.6538);
        direccion.setLongitud(-58.4478);
        entidad.setDireccion(direccion);
        return entidad;
    }

    private static DonacionDTO donacion() {
        DonacionDTO donacion = new DonacionDTO();
        donacion.setId(UUID.randomUUID());
        donacion.setDescripcion("Alimentos no perecederos");
        donacion.setEstado("EN_TRASLADO");
        donacion.setEntidadBeneficiaria("Comedor Los Piletones");
        return donacion;
    }

    private static SeguimientoDTO seguimiento() {
        SeguimientoDTO dto = new SeguimientoDTO();
        dto.setDepositoLat(-34.5986317);
        dto.setDepositoLng(-58.4226973);
        dto.setDepositoEtiqueta("Depósito");
        SeguimientoDTO.CamionEnRuta camion = new SeguimientoDTO.CamionEnRuta();
        camion.setPatente("AC482KP");
        camion.setEstadoRuta("EN_CURSO");
        camion.setLat(-34.6538);
        camion.setLng(-58.4478);
        camion.setDestino("Av. Perito Moreno 2400");
        camion.setParadaActual(1);
        camion.setTrazado(List.of(
                List.of(-58.4226973, -34.5986317),
                List.of(-58.4478, -34.6538)));
        dto.setCamiones(List.of(camion));
        return dto;
    }

    private static MockHttpSession sessionDonante() {
        OidcUser user = new OidcUser() {
            @Override public Map<String, Object> getClaims() { return Map.of("email", "donante@ddsi.local"); }
            @Override public OidcUserInfo getUserInfo() { return null; }
            @Override public OidcIdToken getIdToken() { return null; }
            @Override public Map<String, Object> getAttributes() { return getClaims(); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_DONANTE"));
            }
            @Override public String getName() { return "donante@ddsi.local"; }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "password", List.of(new SimpleGrantedAuthority("ROLE_DONANTE"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
