package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.DonacionDestacadaDTO;
import ar.edu.utn.frba.ddsi.front.dto.PageResponse;
import ar.edu.utn.frba.ddsi.front.dto.incentivos.IncentivosAdminDTOs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class IncentivosClient {

    private static final Logger log = LoggerFactory.getLogger(IncentivosClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public IncentivosClient(RestClient restClient,
                            @Value("${servicio.incentivos.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    /** Baja lógica e idempotente del perfil en incentivos. */
    public void eliminarPerfil(UUID idUsuario) {
        restClient.delete()
                .uri(baseUrl + "/api/perfiles/sincronizacion/{idUsuario}", idUsuario)
                .retrieve()
                .toBodilessEntity();
    }

    /** Últimas donaciones entregadas que completaron una misión. Nunca propaga la falla. */
    public List<DonacionDestacadaDTO> obtenerDonacionesDestacadas() {
        try {
            List<DonacionDestacadaDTO> destacadas = restClient.get()
                    .uri(baseUrl + "/api/donaciones/destacadas")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DonacionDestacadaDTO>>() {});
            return destacadas != null ? destacadas : Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener donaciones destacadas desde {}: {}",
                    baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> obtenerPerfilPublico(String idUsuario) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/perfiles/{idUsuario}/publico", idUsuario)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error al obtener perfil público {} desde {}: {}", idUsuario, baseUrl, e.getMessage());
            return Collections.emptyMap();
        }
    }

    public Map<String, Object> obtenerMision(String idUsuario) {
        return obtenerMapa("/api/perfiles/" + idUsuario + "/mision", "misión");
    }

    public List<Map<String, Object>> obtenerInsignias(String idUsuario) {
        try {
            Map<String, Object> pagina = restClient.get()
                    .uri(baseUrl + "/api/perfiles/{idUsuario}/insignias?page=0&size=100", idUsuario)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            Object content = pagina == null ? null : pagina.get("content");
            if (content instanceof List<?> elementos) {
                return elementos.stream()
                        .filter(Map.class::isInstance)
                        .map(elemento -> (Map<String, Object>) elemento)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("No se pudieron obtener insignias del perfil {}: {}", idUsuario, e.getMessage());
        }
        return Collections.emptyList();
    }

    public Map<String, Object> obtenerPuestoRanking(String idUsuario) {
        return obtenerMapa("/api/rankings/" + idUsuario + "/puestoRanking", "puesto del ranking");
    }

    public Map<String, Object> obtenerRankingActual() {
        return obtenerMapa("/api/rankings/actual", "ranking mensual");
    }

    private Map<String, Object> obtenerMapa(String path, String descripcion) {
        try {
            Map<String, Object> respuesta = restClient.get()
                    .uri(baseUrl + path)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            return respuesta == null ? Collections.emptyMap() : respuesta;
        } catch (Exception e) {
            log.warn("No se pudo obtener {} desde {}: {}", descripcion, baseUrl, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /** Misiones para la pantalla de administración. */
    public List<IncentivosAdminDTOs.Mision> obtenerMisiones() {
        try {
            int pageSize = 100;
            PageResponse<IncentivosAdminDTOs.Mision> page = restClient.get()
                    .uri(baseUrl + "/api/misiones?page=0&size={size}", pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<IncentivosAdminDTOs.Mision>>() {});
            if (page == null) {
                return List.of();
            }
            List<IncentivosAdminDTOs.Mision> misiones = new ArrayList<>(page.getContent());
            for (int numeroPagina = 1; numeroPagina < page.getTotalPages(); numeroPagina++) {
                PageResponse<IncentivosAdminDTOs.Mision> siguiente = restClient.get()
                        .uri(baseUrl + "/api/misiones?page={page}&size={size}", numeroPagina, pageSize)
                        .retrieve()
                        .body(new ParameterizedTypeReference<PageResponse<IncentivosAdminDTOs.Mision>>() {});
                if (siguiente != null) {
                    misiones.addAll(siguiente.getContent());
                }
            }
            return misiones;
        } catch (Exception e) {
            log.error("Error al obtener misiones desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    /** Actualiza una misión desde la pantalla de administración. */
    public void actualizarMision(UUID idAdmin, UUID idMision, IncentivosAdminDTOs.Mision mision) {
        restClient.put()
                .uri(baseUrl + "/api/misiones/admin/{id}", idMision)
                .header("X-Admin-Id", idAdmin.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(mision)
                .retrieve()
                .toBodilessEntity();
    }

    /** Categorías para la pantalla de administración. */
    public List<IncentivosAdminDTOs.Categoria> obtenerCategorias() {
        try {
            int pageSize = 100;
            PageResponse<IncentivosAdminDTOs.Categoria> page = restClient.get()
                    .uri(baseUrl + "/api/categorias?page=0&size={size}", pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<IncentivosAdminDTOs.Categoria>>() {});
            if (page == null) {
                return List.of();
            }
            List<IncentivosAdminDTOs.Categoria> categorias = new ArrayList<>(page.getContent());
            for (int numeroPagina = 1; numeroPagina < page.getTotalPages(); numeroPagina++) {
                PageResponse<IncentivosAdminDTOs.Categoria> siguiente = restClient.get()
                        .uri(baseUrl + "/api/categorias?page={page}&size={size}", numeroPagina, pageSize)
                        .retrieve()
                        .body(new ParameterizedTypeReference<PageResponse<IncentivosAdminDTOs.Categoria>>() {});
                if (siguiente != null) {
                    categorias.addAll(siguiente.getContent());
                }
            }
            return categorias;
        } catch (Exception e) {
            log.error("Error al obtener categorías desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    public void crearCategoria(IncentivosAdminDTOs.Categoria categoria, UUID idAdmin) {
        restClient.post()
                .uri(baseUrl + "/api/categorias/admin")
                .header("Admin-Id", idAdmin.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(categoria)
                .retrieve()
                .toBodilessEntity();
    }

    public void actualizarCategoria(UUID idCategoria,
                                    IncentivosAdminDTOs.Categoria categoria,
                                    UUID idAdmin) {
        restClient.put()
                .uri(baseUrl + "/api/categorias/admin/{id}", idCategoria)
                .header("Admin-Id", idAdmin.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(categoria)
                .retrieve()
                .toBodilessEntity();
    }

    public void eliminarCategoria(UUID idCategoria, UUID idAdmin) {
        restClient.delete()
                .uri(baseUrl + "/api/categorias/admin/{id}", idCategoria)
                .header("Admin-Id", idAdmin.toString())
                .retrieve()
                .toBodilessEntity();
    }

    /** Historial de rankings mensuales. */
    public List<IncentivosAdminDTOs.RankingMes> obtenerRankings() {
        try {
            PageResponse<IncentivosAdminDTOs.RankingMes> page = restClient.get()
                    .uri(baseUrl + "/api/rankings?page=0&size=100")
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<IncentivosAdminDTOs.RankingMes>>() {});
            return page != null && page.getContent() != null ? page.getContent() : List.of();
        } catch (Exception e) {
            log.error("Error al obtener rankings desde {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }
}
