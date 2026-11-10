package ar.edu.utn.frba.ddsi.front.clients;

import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.FormularioDonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.MediosContactoDTO;
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

    public void registrarUsuario(String tipoRegistro, Map<String, String> datos) {
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
                        "numeroDeDocumento", "genero");
                payload.put("medioDeContacto", contacto(datos));
                endpoint = "/api/admins";
            }
            case "DONANTE" -> {
                String tipoPersona = datos.get("tipoPersona");
                if (!"HUMANA".equals(tipoPersona) && !"JURIDICA".equals(tipoPersona)) {
                    throw new IllegalArgumentException("Seleccioná un tipo de persona válido.");
                }
                requerir(datos, "calleUno", "altura", "ciudad", "provincia", "pais");
                payload = new LinkedHashMap<>();
                payload.put("tipoPersona", tipoPersona);
                if ("HUMANA".equals(tipoPersona)) {
                    requerir(datos, "nombre", "apellido", "edad", "numeroDeDocumento", "genero");
                    copiarCamposAObjeto(datos, payload, "nombre", "apellido", "edad", "numeroDeDocumento", "genero");
                } else {
                    requerir(datos, "razonSocial", "rubro", "cuit", "tipoJuridico");
                    copiarCamposAObjeto(datos, payload, "razonSocial", "rubro", "cuit", "tipoJuridico");
                }
                Map<String, String> medio = contactoOpcional(datos);
                if (medio != null) {
                    payload.put("medioPredeterminado", medio);
                    payload.put("mediosDeContacto", List.of(medio));
                }
                payload.put("direccion", direccion(datos));
                endpoint = "/api/personas";
            }
            case "ENTIDAD" -> {
                requerir(datos, "razonSocial", "rubro", "cuit", "tipoJuridico",
                        "calleUno", "altura", "ciudad", "provincia", "pais", "correoElectronico");
                payload = new LinkedHashMap<>();
                copiarCamposAObjeto(datos, payload, "razonSocial", "rubro", "cuit", "tipoJuridico",
                        "telefono", "correoElectronico");
                payload.put("direccion", direccion(datos));
                endpoint = "/api/entidades";
            }
            default -> throw new IllegalArgumentException("Seleccioná un tipo de registro válido.");
        }

        restClient.post()
                .uri(baseUrl + endpoint)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }

    private static Map<String, String> contacto(Map<String, String> datos) {
        return Map.of("tipo", datos.get("tipoContacto"), "valor", datos.get("valorContacto"));
    }

    private static Map<String, String> contactoOpcional(Map<String, String> datos) {
        String tipo = datos.get("tipoContacto");
        String valor = datos.get("valorContacto");
        if ((tipo == null || tipo.isBlank()) && (valor == null || valor.isBlank())) {
            return null;
        }
        if (tipo == null || tipo.isBlank() || valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Completá el tipo y el valor del medio de contacto.");
        }
        return contacto(datos);
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
