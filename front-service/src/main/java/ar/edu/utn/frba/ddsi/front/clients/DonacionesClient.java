package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.AdminDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.IdentidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.front.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.PageResponse;
import ar.edu.utn.frba.ddsi.front.dto.PersonaDonanteDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class DonacionesClient {

    private static final Logger log = LoggerFactory.getLogger(DonacionesClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public DonacionesClient(RestClient restClient,
                            @Value("${servicio.donaciones.url}") String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public List<DonacionDTO> obtenerTodasLasDonaciones() {
        try {
            int pageSize = 100;
            PageResponse<DonacionDTO> page = restClient.get()
                    .uri(baseUrl + "/api/donaciones?page=0&size={size}", pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<DonacionDTO>>() {});
            if (page == null) {
                return Collections.emptyList();
            }

            List<DonacionDTO> donaciones = new ArrayList<>(page.getContent());
            for (int pageNumber = 1; pageNumber < page.getTotalPages(); pageNumber++) {
                PageResponse<DonacionDTO> nextPage = restClient.get()
                        .uri(baseUrl + "/api/donaciones?page={page}&size={size}", pageNumber, pageSize)
                        .retrieve()
                        .body(new ParameterizedTypeReference<PageResponse<DonacionDTO>>() {});
                if (nextPage != null) {
                    donaciones.addAll(nextPage.getContent());
                }
            }
            return donaciones;
        } catch (Exception e) {
            log.error("Error al obtener donaciones desde {}: {}", baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    public DonacionDTO obtenerDonacionPorId(UUID id) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/donaciones/{id}", id)
                    .retrieve()
                    .body(DonacionDTO.class);
        } catch (Exception e) {
            log.error("Error al obtener donación con id {} desde {}: {}", id, baseUrl, e.getMessage());
            return null;
        }
    }

    public List<PersonaDonanteDTO> obtenerDonantes() {
        try {
            return cargarDonantes();
        } catch (Exception e) {
            log.error("Error al obtener donantes desde {}: {}", baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Estado del donante en donaciones. Un 404 se traduce a {@code false}; una falla de
     * infraestructura a {@link Optional#empty()} para no reconciliar durante una caída.
     */
    public Optional<Boolean> donanteSigueActivo(UUID id) {
        try {
            PersonaDonanteDTO donante = restClient.get()
                    .uri(baseUrl + "/api/personas/{id}", id)
                    .retrieve()
                    .body(PersonaDonanteDTO.class);
            if (donante == null || donante.getActivo() == null) {
                return Optional.empty();
            }
            return Optional.of(donante.getActivo());
        } catch (org.springframework.web.client.RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return Optional.of(false);
            }
            log.error("No se pudo consultar el donante {} en {}: {}", id, baseUrl, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("No se pudo consultar el donante {} en {}: {}", id, baseUrl, e.getMessage());
            return Optional.empty();
        }
    }

    /** Busca donantes con un medio de contacto EMAIL/MAIL que matchee el correo (case-insensitive). */
    public List<PersonaDonanteDTO> buscarDonantesPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) {
            return Collections.emptyList();
        }

        return cargarDonantes().stream()
                .filter(donante -> donante.getMediosDeContacto() != null)
                .filter(donante -> donante.getMediosDeContacto().stream()
                        .filter(contacto -> contacto != null)
                        .anyMatch(contacto -> ("EMAIL".equalsIgnoreCase(contacto.getTipo())
                                || "MAIL".equalsIgnoreCase(contacto.getTipo()))
                                && contacto.getValor() != null
                                && contacto.getValor().trim().equalsIgnoreCase(correo.trim())))
                .toList();
    }

    /** Busca el perfil de donante por su ID de dominio. */
    public PersonaDonanteDTO obtenerDonantePorId(UUID id) {
        if (id == null) {
            return null;
        }
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/personas/{id}", id)
                    .retrieve()
                    .body(PersonaDonanteDTO.class);
        } catch (Exception e) {
            log.warn("No se pudo obtener el perfil de donante {} desde {}: {}", id, baseUrl, e.getMessage());
            return null;
        }
    }

    private List<PersonaDonanteDTO> cargarDonantes() {
        int pageSize = 100;
        PageResponse<PersonaDonanteDTO> page = restClient.get()
                .uri(baseUrl + "/api/personas?page=0&size={size}", pageSize)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse<PersonaDonanteDTO>>() {});
        if (page == null) {
            return Collections.emptyList();
        }

        List<PersonaDonanteDTO> donantes = new ArrayList<>(page.getContent());
        for (int pageNumber = 1; pageNumber < page.getTotalPages(); pageNumber++) {
            PageResponse<PersonaDonanteDTO> nextPage = restClient.get()
                    .uri(baseUrl + "/api/personas?page={page}&size={size}", pageNumber, pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<PersonaDonanteDTO>>() {});
            if (nextPage != null) {
                donantes.addAll(nextPage.getContent());
            }
        }
        return donantes;
    }

    public void crearDonacion(FormularioDonacionDTO formulario) {
        restClient.post()
                .uri(baseUrl + "/api/donaciones/formulario")
                .body(formulario)
                .retrieve()
                .toBodilessEntity();
    }

    /** Devuelve el UUID del perfil creado (donante/entidad) o null si no se pudo determinar. */
    public UUID registrarUsuario(String tipoRegistro, Map<String, String> datos) {
        return registrarUsuario(tipoRegistro, datos, null);
    }

    /** Variante que vincula el administrador con su cuenta de Keycloak. */
    public UUID registrarUsuario(String tipoRegistro, Map<String, String> datos, String idKeycloak) {
        if (tipoRegistro == null || tipoRegistro.isBlank()) {
            throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        }

        Map<String, Object> payload;
        String endpoint;

        switch (tipoRegistro) {
            case "ADMIN" -> {
                requerir(datos, "nombreAMostrar", "nombre", "apellido", "edad", "numeroDeDocumento",
                        "genero", "tipoContacto", "valorContacto");
                payload = new LinkedHashMap<>();
                copiarCamposAObjeto(datos, payload, "nombreAMostrar", "nombre", "apellido", "edad",
                        "numeroDeDocumento", "genero", "email");
                if (idKeycloak != null && !idKeycloak.isBlank()) {
                    payload.put("id", idKeycloak.trim());
                }
                payload.put("medioDeContacto", contacto(datos));
                endpoint = "/api/admins";
            }
            case "DONANTE" -> {
                String tipoPersona = datos.get("tipoPersona");
                if (!"HUMANA".equals(tipoPersona) && !"JURIDICA".equals(tipoPersona)) {
                    throw new IllegalArgumentException("Seleccioná un tipo de persona válido.");
                }
                requerir(datos, "email");
                payload = new LinkedHashMap<>();
                payload.put("tipoPersona", tipoPersona);
                if ("HUMANA".equals(tipoPersona)) {
                    requerir(datos, "nombre", "apellido", "edad", "numeroDeDocumento", "genero");
                    copiarCamposAObjeto(datos, payload, "nombre", "apellido", "edad", "numeroDeDocumento", "genero");
                } else {
                    requerir(datos, "razonSocial", "rubro", "cuit", "tipoJuridico");
                    copiarCamposAObjeto(datos, payload, "razonSocial", "rubro", "cuit", "tipoJuridico");
                }
                // El correo de la cuenta queda siempre como medio de contacto para poder
                // vincular el perfil con donante/admin/entidad.
                List<Map<String, String>> medios = new ArrayList<>();
                Map<String, String> medioElegido = contactoOpcional(datos);
                if (medioElegido != null) {
                    medios.add(medioElegido);
                }
                String email = datos.get("email");
                if (email != null && !email.isBlank()) {
                    String normalizado = email.trim().toLowerCase();
                    boolean yaIncluido = medios.stream().anyMatch(m ->
                            "EMAIL".equalsIgnoreCase(m.get("tipo")) && normalizado.equalsIgnoreCase(m.get("valor")));
                    if (!yaIncluido) {
                        medios.add(Map.of("tipo", "EMAIL", "valor", normalizado));
                    }
                }
                if (!medios.isEmpty()) {
                    payload.put("mediosDeContacto", medios);
                    payload.put("medioPredeterminado", medioElegido != null ? medioElegido : medios.get(medios.size() - 1));
                }
                // La dirección es opcional para donantes: solo se envía si hay algo cargado.
                Map<String, Object> direccionDonante = direccion(datos);
                if (!direccionDonante.isEmpty()) {
                    payload.put("direccion", direccionDonante);
                }
                endpoint = "/api/personas";
            }
            case "ENTIDAD" -> {
                requerir(datos, "razonSocial", "rubro", "cuit", "tipoJuridico",
                        "calleUno", "altura", "ciudad", "provincia", "pais");
                // El email obligatorio es el de la cuenta (datos de usuario); correoElectronico
                // es alias de compatibilidad con Sofi y apunta al mismo Mail.
                String emailEntidad = primeroNoVacio(datos.get("email"), datos.get("correoElectronico"));
                if (emailEntidad == null) {
                    throw new IllegalArgumentException("Completá todos los campos obligatorios.");
                }
                payload = new LinkedHashMap<>();
                copiarCamposAObjeto(datos, payload, "razonSocial", "rubro", "cuit", "tipoJuridico", "telefono");
                String emailEntidadNormalizado = emailEntidad.trim().toLowerCase();
                payload.put("email", emailEntidadNormalizado);
                payload.put("correoElectronico", emailEntidadNormalizado);
                payload.put("direccion", direccion(datos));
                endpoint = "/api/entidades";
            }
            default -> throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        }

        try {
            String body = restClient.post()
                    .uri(baseUrl + endpoint)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            return extraerUuid(body);
        } catch (Exception e) {
            log.error("Error al registrar el perfil en {} (endpoint {}): {}", baseUrl, endpoint, e.getMessage());
            throw e;
        }
    }

    private static UUID extraerUuid(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            if (node.hasNonNull("id")) {
                return UUID.fromString(node.get("id").asText());
            }
            return null;
        } catch (Exception e) {
            log.warn("No se pudo parsear el id de la respuesta del perfil: {}", e.getMessage());
            return null;
        }
    }

    /** Perfiles de donante/entidad/admin vinculados a un email; null si Donaciones no responde. */
    public IdentidadDTO buscarIdentidadPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/identidades/por-email?email={email}", email.trim().toLowerCase())
                    .retrieve()
                    .body(IdentidadDTO.class);
        } catch (Exception e) {
            log.warn("No se pudo resolver la identidad por email {}: {}", email, e.getMessage());
            return null;
        }
    }

    public List<DonacionDTO> obtenerDonacionesDeDonante(UUID idDonante) {
        try {
            DonacionDTO[] donaciones = restClient.get()
                    .uri(baseUrl + "/api/donaciones/donante/{id}", idDonante)
                    .retrieve()
                    .body(DonacionDTO[].class);
            return donaciones == null ? Collections.emptyList() : List.of(donaciones);
        } catch (Exception e) {
            log.error("Error al obtener donaciones del donante {} desde {}: {}", idDonante, baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    public AdminDTO obtenerAdminPorId(UUID id) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/admins/{id}", id)
                    .retrieve()
                    .body(AdminDTO.class);
        } catch (Exception e) {
            log.error("Error al obtener admin con id {} desde {}: {}", id, baseUrl, e.getMessage());
            return null;
        }
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/api/entidades/{id}", id)
                    .retrieve()
                    .body(EntidadBeneficiariaDTO.class);
        } catch (Exception e) {
            log.error("Error al obtener entidad con id {} desde {}: {}", id, baseUrl, e.getMessage());
            return null;
        }
    }

    public List<DonacionDTO> obtenerDonacionesDeEntidad(UUID idEntidad) {
        try {
            DonacionDTO[] donaciones = restClient.get()
                    .uri(baseUrl + "/api/entidades/{id}/donaciones", idEntidad)
                    .retrieve()
                    .body(DonacionDTO[].class);
            return donaciones == null ? Collections.emptyList() : List.of(donaciones);
        } catch (Exception e) {
            log.error("Error al obtener donaciones de la entidad {} desde {}: {}", idEntidad, baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

    public NecesidadDTO crearNecesidad(UUID idEntidad, NecesidadDTO dto) {
        try {
            return restClient.post()
                    .uri(baseUrl + "/api/entidades/{id}/necesidades", idEntidad)
                    .body(dto)
                    .retrieve()
                    .body(NecesidadDTO.class);
        } catch (Exception e) {
            log.error("Error al crear necesidad en la entidad {}: {}", idEntidad, e.getMessage());
            throw e;
        }
    }

    public NecesidadDTO actualizarNecesidad(UUID idEntidad, UUID idNecesidad, NecesidadDTO dto) {
        try {
            return restClient.put()
                    .uri(baseUrl + "/api/entidades/{id}/necesidades/{idNecesidad}", idEntidad, idNecesidad)
                    .body(dto)
                    .retrieve()
                    .body(NecesidadDTO.class);
        } catch (Exception e) {
            log.error("Error al actualizar la necesidad {} de la entidad {}: {}", idNecesidad, idEntidad, e.getMessage());
            throw e;
        }
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        try {
            restClient.delete()
                    .uri(baseUrl + "/api/entidades/{id}/necesidades/{idNecesidad}", idEntidad, idNecesidad)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Error al eliminar la necesidad {} de la entidad {}: {}", idNecesidad, idEntidad, e.getMessage());
            throw e;
        }
    }

    private static String primeroNoVacio(String... valores) {
        if (valores == null) {
            return null;
        }
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static Map<String, String> contacto(Map<String, String> datos) {
        return Map.of("tipo", datos.get("tipoContacto"), "valor", datos.get("valorContacto"));
    }

    private static Map<String, String> contactoOpcional(Map<String, String> datos) {
        String valor = datos.get("valorContacto");
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String tipo = datos.get("tipoContacto");
        if (tipo == null || tipo.isBlank()) {
            tipo = "EMAIL";
        }
        return Map.of("tipo", tipo.trim().toUpperCase(), "valor", valor.trim());
    }

    private static Map<String, Object> direccion(Map<String, String> datos) {
        Map<String, Object> direccion = new LinkedHashMap<>();
        copiarCamposAObjeto(datos, direccion, "calleUno", "calleDos", "altura", "piso", "departamento",
                "ciudad", "provincia", "pais");
        return direccion;
    }

    private static void copiarCamposAObjeto(Map<String, String> origen, Map<String, Object> destino, String... campos) {
        for (String campo : campos) {
            String valor = origen.get(campo);
            if (valor != null && !valor.isBlank()) {
                destino.put(campo, valor);
            }
        }
    }

    private static void requerir(Map<String, String> datos, String... campos) {
        for (String campo : campos) {
            if (datos.get(campo) == null || datos.get(campo).isBlank()) {
                throw new IllegalArgumentException("Completá todos los campos obligatorios.");
            }
        }
    }

    /** Necesidades de una entidad; la entidad las trae aparte para no serializar el grafo completo. */
    public List<NecesidadDTO> obtenerNecesidadesDeEntidad(UUID idEntidad) {
        try {
            NecesidadDTO[] necesidades = restClient.get()
                    .uri(baseUrl + "/api/entidades/{id}/necesidades", idEntidad)
                    .retrieve()
                    .body(NecesidadDTO[].class);
            return necesidades == null ? Collections.emptyList() : List.of(necesidades);
        } catch (Exception e) {
            log.warn("No se pudieron obtener las necesidades de la entidad {}: {}", idEntidad, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<EntidadBeneficiariaDTO> obtenerEntidades() {
        try {
            int pageSize = 100;
            PageResponse<EntidadBeneficiariaDTO> page = restClient.get()
                    .uri(baseUrl + "/api/entidades?page=0&size={size}", pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<EntidadBeneficiariaDTO>>() {});
            if (page == null) {
                return Collections.emptyList();
            }

            List<EntidadBeneficiariaDTO> entidades = new ArrayList<>(page.getContent());
            for (int pageNumber = 1; pageNumber < page.getTotalPages(); pageNumber++) {
                PageResponse<EntidadBeneficiariaDTO> nextPage = restClient.get()
                        .uri(baseUrl + "/api/entidades?page={page}&size={size}", pageNumber, pageSize)
                        .retrieve()
                        .body(new ParameterizedTypeReference<PageResponse<EntidadBeneficiariaDTO>>() {});
                if (nextPage != null) {
                    entidades.addAll(nextPage.getContent());
                }
            }
            return entidades;
        } catch (Exception e) {
            log.error("Error al obtener entidades desde {}: {}", baseUrl, e.getMessage());
            return Collections.emptyList();
        }
    }

}
