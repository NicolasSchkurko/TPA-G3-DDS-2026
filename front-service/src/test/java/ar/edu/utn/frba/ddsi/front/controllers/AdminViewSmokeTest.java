package ar.edu.utn.frba.ddsi.front.controllers;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.incentivos.IncentivosAdminDTOs;
import ar.edu.utn.frba.ddsi.front.services.AdminResumenService;
import ar.edu.utn.frba.ddsi.front.services.SeguimientoTokenService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminViewSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DonacionesClient donacionesClient;

    @MockBean
    private IncentivosClient incentivosClient;

    @MockBean
    private LogisticasClient logisticasClient;

    @MockBean
    private AdminResumenService adminResumenService;

    @MockBean
    private SeguimientoTokenService seguimientoTokenService;

    @Test
    void adminViewsRenderWithoutBrokenTemplates() throws Exception {
        UUID idMision = UUID.randomUUID();
        IncentivosAdminDTOs.Mision mision = new IncentivosAdminDTOs.Mision();
        mision.setIdMision(idMision);
        mision.setNombreMision("Primera misión");
        mision.setDescripcion("Descripción");

        IncentivosAdminDTOs.Categoria categoria = new IncentivosAdminDTOs.Categoria();
        categoria.setIdCategoria(UUID.randomUUID());
        categoria.setNombre("Principiantes");
        categoria.setPosicionSecuencia(1);
        categoria.setMisiones(List.of(idMision));

        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerEntidades()).thenReturn(List.of());
        when(donacionesClient.obtenerTodasLasDonaciones()).thenReturn(List.of());
        when(incentivosClient.obtenerMisiones()).thenReturn(List.of(mision));
        when(incentivosClient.obtenerCategorias()).thenReturn(List.of(categoria));
        when(logisticasClient.obtenerCamiones()).thenReturn(List.of());
        when(logisticasClient.obtenerRutas()).thenReturn(List.of());
        when(logisticasClient.obtenerEntregasNoRecibidas()).thenReturn(null);
        when(logisticasClient.obtenerRutasActivas()).thenReturn(List.of());

        when(donacionesClient.obtenerResultadosMatchmaking()).thenReturn(List.of());
        when(adminResumenService.obtenerResumen()).thenReturn(new AdminResumenService.Resumen());
        when(donacionesClient.obtenerCatalogoCategorias()).thenReturn(CATALOGO);
        when(donacionesClient.obtenerDonacionPorId(ID_DONACION)).thenReturn(donacionEnDeposito());

        for (String path : List.of("/admin", "/admin/donaciones", "/admin/donaciones?vista=matchmaking",
                "/admin/entidades", "/admin/donantes", "/admin/donaciones/nuevo",
                "/admin/donaciones/" + ID_DONACION + "/editar",
                "/admin/donantes/nuevo", "/admin/entidades/nuevo", "/admin/camiones",
                "/admin/camiones/nuevo", "/admin/choferes", "/admin/choferes/nuevo",
                "/admin/rutas", "/admin/entregas-no-recibidas",
                "/admin/donaciones?vista=vencidas", "/admin/donantes/importar/" + java.util.UUID.randomUUID(),
                "/admin/incentivos", "/admin/incentivos/rankings")) {
            mockMvc.perform(get(path).session(sessionAdmin()))
                    .andExpect(status().isOk());
        }

        String rankingsPage = mockMvc.perform(get("/admin/incentivos/rankings").session(sessionAdmin()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(rankingsPage).contains("max=\"" + java.time.YearMonth.now() + "\"");
        assertThat(rankingsPage).contains("snapshot parcial");

        String incentivesPage = mockMvc.perform(get("/admin/incentivos")
                        .session(sessionAdmin())
                        .flashAttr("toastMensaje", "No se pudo eliminar la categoría")
                        .flashAttr("toastTipo", "error"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(incentivesPage).containsPattern(
                "(?s)<form method=\"post\" id=\"deleteForm\" action=\"#\">\\s*"
                        + "<input type=\"hidden\" name=\"_csrf\" value=\"[^\"]+\"");
        assertThat(incentivesPage).contains("src=\"/js/core/toast.js\"");
    }

    /**
     * Categoría y subcategoría tienen que ser selects alimentados por el catálogo, no texto libre.
     *
     * Esto no es cosmético: el asignador empareja donaciones con necesidades comparando el NOMBRE
     * de la subcategoría con equals(), así que un input de texto permite escribir "Frazada" donde
     * la necesidad dice "Frazadas" y la donación queda en depósito para siempre, sin error visible.
     * Si alguien vuelve a poner un <input type="text"> acá, este test tiene que fallar.
     */
    @Test
    void donationFormsOfferCatalogSelectsInsteadOfFreeText() throws Exception {
        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerCatalogoCategorias()).thenReturn(CATALOGO);
        when(donacionesClient.obtenerDonacionPorId(ID_DONACION)).thenReturn(donacionEnDeposito());

        for (String path : List.of("/admin/donaciones/nuevo",
                "/admin/donaciones/" + ID_DONACION + "/editar")) {
            String html = mockMvc.perform(get(path).session(sessionAdmin()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(html)
                    .as("%s tiene que usar selects del catálogo para categoría y subcategoría", path)
                    .contains("selector-categoria-bien")
                    .contains("selector-subcategoria-bien")
                    .doesNotContain("data-campo=\"categoria\" placeholder")
                    .doesNotContain("type=\"text\" class=\"form-control\" data-campo=\"categoria\"")
                    .doesNotContain("type=\"text\" class=\"form-control\" data-campo=\"subcategoria\"");

            // Las opciones salen del catálogo real, y la subcategoría se renderiza completa para que
            // th:field pueda marcar la que el bien ya tenía (el JS la filtra después por categoría).
            assertThat(html).as("%s tiene que listar las categorías del catálogo", path)
                    .contains("value=\"Abrigo\"")
                    .contains("value=\"ALIMENTOS\"")
                    .contains("value=\"Frazadas\"")
                    .contains("value=\"Leche en polvo\"");

            // El JS necesita el catálogo para encadenar los dos selects.
            assertThat(html).as("%s tiene que publicar window.CATALOGO", path)
                    .contains("window.CATALOGO");
        }
    }

    /** Sin catálogo el alta avisa en vez de dejar elegir nada: el backend rechazaría el alta igual. */
    @Test
    void donationFormWarnsWhenCatalogIsEmpty() throws Exception {
        when(donacionesClient.obtenerDonantes()).thenReturn(List.of());
        when(donacionesClient.obtenerCatalogoCategorias()).thenReturn(Map.of());

        String html = mockMvc.perform(get("/admin/donaciones/nuevo").session(sessionAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("El catálogo de categorías está vacío");
    }

    private static final UUID ID_DONACION = UUID.fromString("9b3520f3-99c7-42e1-ad1d-04dc0b7b31d6");

    private static final Map<String, List<String>> CATALOGO = new java.util.LinkedHashMap<>(Map.of(
            "Abrigo", List.of("Frazadas"),
            "ALIMENTOS", List.of("Arroz", "Leche en polvo")));

    /** EN_DEPOSITO para que la pantalla de edición muestre los bienes editables (los selects). */
    private static ar.edu.utn.frba.ddsi.front.dto.DonacionDTO donacionEnDeposito() {
        ar.edu.utn.frba.ddsi.front.dto.DonacionDTO donacion =
                new ar.edu.utn.frba.ddsi.front.dto.DonacionDTO();
        donacion.setId(ID_DONACION);
        donacion.setDonanteName("Mateo Galanti");
        donacion.setDescripcion("Segmento de donación: Frazadas");
        donacion.setEstado("EN_DEPOSITO");
        donacion.setCategoriaBienName("Abrigo");
        donacion.setSubcategoriaName("Frazadas");
        donacion.setCantidadTotalBienes(30);

        ar.edu.utn.frba.ddsi.front.dto.BienDonacionDTO bien =
                new ar.edu.utn.frba.ddsi.front.dto.BienDonacionDTO();
        bien.setDescripcion("frazadas");
        bien.setCategoria("Abrigo");
        bien.setSubcategoria("Frazadas");
        bien.setCantidad(30);
        bien.setUnidadDeMedida("UNIDADES");
        bien.setTipoBien("CON_ESTADO");
        bien.setUsado(false);
        donacion.setBienes(new java.util.ArrayList<>(List.of(bien)));
        return donacion;
    }

    private static MockHttpSession sessionAdmin() {
        OidcUser user = new OidcUser() {
            @Override public Map<String, Object> getClaims() { return Map.of("email", "admin@ddsi.local"); }
            @Override public OidcUserInfo getUserInfo() { return null; }
            @Override public OidcIdToken getIdToken() { return null; }
            @Override public Map<String, Object> getAttributes() { return getClaims(); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            @Override public String getName() { return "admin@ddsi.local"; }
        };
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
