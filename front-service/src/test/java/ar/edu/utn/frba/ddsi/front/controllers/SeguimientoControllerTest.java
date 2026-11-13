package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.services.ChoferPortalService;
import ar.edu.utn.frba.ddsi.front.services.IdentidadService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Seguimiento de ruta en el front")
class SeguimientoControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Une las coordenadas de las paradas en un LineString y agrega un punto por parada")
    void armaElGeojsonDesdeLasParadas() throws Exception {
        UUID idRuta = UUID.randomUUID();
        JsonNode ruta = objectMapper.readTree("""
                {
                  "idRuta": "%s",
                  "paradas": [
                    {"direccion": {"latitud": -34.60, "longitud": -58.38}},
                    {"direccion": {"latitud": -34.61, "longitud": -58.42}},
                    {"direccion": {"latitud": null, "longitud": null}}
                  ]
                }
                """.formatted(idRuta));

        LogisticasClient cliente = mock(LogisticasClient.class);
        when(cliente.obtenerRuta(idRuta)).thenReturn(ruta);

        SeguimientoTokenService tokens = new SeguimientoTokenService("test-secret");
        ExtendedModelMap model = new ExtendedModelMap();
        String vista = controller(cliente, tokens)
                .seguimiento(tokens.generar(idRuta), admin(), model);

        assertThat(vista).isEqualTo("seguimiento/ruta");
        assertThat(model.get("hayTrazado")).isEqualTo(true);

        JsonNode geojson = objectMapper.readTree((String) model.get("geojson"));
        assertThat(geojson.path("type").asText()).isEqualTo("FeatureCollection");
        assertThat(geojson.path("features").size()).isEqualTo(3);
        assertThat(geojson.path("features").get(0).path("geometry").path("type").asText())
                .isEqualTo("LineString");
        assertThat(geojson.path("features").get(0).path("geometry").path("coordinates").get(0).get(0).asDouble())
                .isEqualTo(-58.38);
        assertThat(geojson.path("features").get(0).path("geometry").path("coordinates").get(0).get(1).asDouble())
                .isEqualTo(-34.60);
    }

    private SeguimientoController controller(LogisticasClient cliente, SeguimientoTokenService tokens) {
        return controller(cliente, tokens, mock(IdentidadService.class),
                mock(ChoferPortalService.class), mock(DonacionesClient.class));
    }

    private SeguimientoController controller(LogisticasClient cliente, SeguimientoTokenService tokens,
                                             IdentidadService identidad, ChoferPortalService choferes,
                                             DonacionesClient donaciones) {
        return new SeguimientoController(cliente, tokens, objectMapper, identidad, choferes, donaciones);
    }

    private static Authentication admin() {
        return new UsernamePasswordAuthenticationToken("admin", "x",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }


    @Test
    @DisplayName("El donante puede abrir una ruta que contiene su donación")
    void donanteConDonacionEnLaRuta() throws Exception {
        UUID idRuta = UUID.randomUUID();
        UUID idDonacion = UUID.randomUUID();
        UUID idDonante = UUID.randomUUID();
        JsonNode ruta = objectMapper.readTree("""
                {
                  "idRuta": "%s",
                  "estado": "EN_CURSO",
                  "paradas": [{"direccion": {"calleUno": "Av. Test", "altura": 1, "latitud": -34.6, "longitud": -58.4},
                               "items": {"idsDonaciones": ["%s"], "bienes": [{"estado": "PENDIENTE"}]}}]
                }
                """.formatted(idRuta, idDonacion));

        LogisticasClient cliente = mock(LogisticasClient.class);
        when(cliente.obtenerRuta(idRuta)).thenReturn(ruta);
        IdentidadService identidad = mock(IdentidadService.class);
        when(identidad.donanteIdActual(any())).thenReturn(java.util.Optional.of(idDonante));
        DonacionesClient donaciones = mock(DonacionesClient.class);
        DonacionDTO donacion = new DonacionDTO();
        donacion.setId(idDonacion);
        when(donaciones.obtenerDonacionesDeDonante(idDonante)).thenReturn(List.of(donacion));

        SeguimientoTokenService tokens = new SeguimientoTokenService("test-secret");
        String vista = controller(cliente, tokens, identidad, mock(ChoferPortalService.class), donaciones)
                .seguimiento(tokens.generar(idRuta), donante(), new ExtendedModelMap());

        assertThat(vista).isEqualTo("seguimiento/ruta");
    }

    @Test
    @DisplayName("Un donante sin donaciones en la ruta no puede abrirla")
    void donanteSinDonacionEnLaRuta() throws Exception {
        UUID idRuta = UUID.randomUUID();
        UUID idDonante = UUID.randomUUID();
        JsonNode ruta = objectMapper.readTree("""
                {"idRuta": "%s", "paradas": [{"direccion": {"calleUno": "Av. Test", "altura": 1,
                 "latitud": -34.6, "longitud": -58.4}, "items": {"idsDonaciones": ["%s"]}}]}
                """.formatted(idRuta, UUID.randomUUID()));

        LogisticasClient cliente = mock(LogisticasClient.class);
        when(cliente.obtenerRuta(idRuta)).thenReturn(ruta);
        IdentidadService identidad = mock(IdentidadService.class);
        when(identidad.donanteIdActual(any())).thenReturn(java.util.Optional.of(idDonante));
        DonacionesClient donaciones = mock(DonacionesClient.class);
        when(donaciones.obtenerDonacionesDeDonante(idDonante)).thenReturn(List.of());

        SeguimientoTokenService tokens = new SeguimientoTokenService("test-secret");
        String vista = controller(cliente, tokens, identidad, mock(ChoferPortalService.class), donaciones)
                .seguimiento(tokens.generar(idRuta), donante(), new ExtendedModelMap());

        assertThat(vista).isEqualTo("redirect:/sin-acceso");
    }

    private static Authentication donante() {
        return new UsernamePasswordAuthenticationToken("donante", "x",
                List.of(new SimpleGrantedAuthority("ROLE_DONANTE")));
    }

    @Test
    @DisplayName("Una ruta inexistente vuelve al inicio")
    void rutaInexistenteNoRompe() throws Exception {
        UUID idRuta = UUID.randomUUID();
        LogisticasClient cliente = mock(LogisticasClient.class);
        when(cliente.obtenerRuta(idRuta)).thenReturn(null);

        SeguimientoTokenService tokens = new SeguimientoTokenService("test-secret");
        ExtendedModelMap model = new ExtendedModelMap();
        String vista = controller(cliente, tokens)
                .seguimiento(tokens.generar(idRuta), admin(), model);

        assertThat(vista).isEqualTo("redirect:/");
    }
}
