package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDTO;
import ar.edu.utn.frba.ddsi.front.dto.ChoferDashboardDTO;
import ar.edu.utn.frba.ddsi.front.dto.PageResponse;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Resuelve el chofer autenticado y arma el estado de su jornada:
 * camión asignado, ruta activa, paradas, entregas e historial paginado.
 */
@Service
public class ChoferPortalService {

    private static final int HISTORIAL_PAGE_SIZE = 8;

    private final LogisticasClient logisticasClient;
    private final ObjectMapper objectMapper;
    private final SeguimientoTokenService tokenService;

    public ChoferPortalService(LogisticasClient logisticasClient,
                               ObjectMapper objectMapper,
                               SeguimientoTokenService tokenService) {
        this.logisticasClient = logisticasClient;
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
    }

    public ChoferDashboardDTO construir(Authentication authentication, int historialPagina) {
        ChoferDashboardDTO dashboard = new ChoferDashboardDTO();
        dashboard.setUsuario(nombreVisible(authentication));

        Optional<ChoferDTO> chofer = choferActual(authentication);
        if (chofer.isEmpty()) {
            return dashboard;
        }

        dashboard.setPerfilEncontrado(true);
        dashboard.setChofer(chofer.get());

        LogisticaDTOs.Ruta activa = logisticasClient.obtenerRutaActivaDelChofer(chofer.get().getIdChofer());
        if (activa != null) {
            activa.setTokenSeguimiento(tokenService.generar(activa.getIdRuta()));
        }
        dashboard.setRutaActiva(activa);
        dashboard.setCamion(camionDe(chofer.get(), activa));

        if (activa != null) {
            construirParadas(dashboard, activa);
            construirDeposito(dashboard, activa);
            construirGeojson(dashboard);
        }

        int pagina = Math.max(0, historialPagina);
        PageResponse<LogisticaDTOs.Ruta> historial =
                logisticasClient.obtenerHistorialDelChofer(chofer.get().getIdChofer(), pagina, HISTORIAL_PAGE_SIZE);

        dashboard.setHistorial(historial.getContent().stream()
                .filter(ruta -> activa == null || !Objects.equals(ruta.getIdRuta(), activa.getIdRuta()))
                .peek(ruta -> ruta.setTokenSeguimiento(tokenService.generar(ruta.getIdRuta())))
                .toList());
        dashboard.setHistorialPagina(pagina);
        dashboard.setHistorialTotalPaginas(historial.getTotalPages());
        dashboard.setHistorialTotalElementos(historial.getTotalElements());
        dashboard.setHistorialTieneAnterior(pagina > 0);
        dashboard.setHistorialTieneSiguiente(pagina + 1 < historial.getTotalPages());

        return dashboard;
    }

    public void iniciarRuta(Authentication authentication) {
        ChoferDTO chofer = choferRequerido(authentication);
        logisticasClient.iniciarRuta(chofer.getIdChofer());
    }

    public void terminarRuta(Authentication authentication) {
        ChoferDTO chofer = choferRequerido(authentication);
        logisticasClient.terminarRuta(chofer.getIdChofer());
    }

    public void confirmarEntrega(Authentication authentication, UUID idDonacion,
                                 String fotoBase64, String observacion) {
        if (fotoBase64 == null || fotoBase64.length() < 100) {
            throw new IllegalArgumentException("Adjuntá una foto de la entrega.");
        }
        if (fotoBase64.length() > 5_500_000) {
            throw new IllegalArgumentException("La foto es demasiado grande. Probá con una imagen más liviana.");
        }

        ChoferDTO chofer = choferRequerido(authentication);
        LogisticaDTOs.Ruta ruta = logisticasClient.obtenerRutaActivaDelChofer(chofer.getIdChofer());
        if (ruta == null) {
            throw new IllegalArgumentException("No tenés una ruta activa para marcar entregas.");
        }
        if (!"EN_CURSO".equalsIgnoreCase(ruta.getEstado())) {
            throw new IllegalArgumentException("Iniciá la ruta antes de marcar entregas.");
        }
        if (!rutaContiene(ruta, idDonacion)) {
            throw new IllegalArgumentException("Esa entrega no pertenece a tu ruta activa.");
        }
        if (!esParadaActual(ruta, idDonacion)) {
            throw new IllegalArgumentException("Primero terminá la parada actual antes de marcar esta entrega.");
        }

        LogisticaDTOs.ActualizacionEntrega actualizacion = new LogisticaDTOs.ActualizacionEntrega();
        actualizacion.setEstado("ENTREGADA");
        actualizacion.setFotoUrl(fotoBase64);
        actualizacion.setJustificacion(observacion != null && !observacion.isBlank() ? observacion : null);
        logisticasClient.actualizarEstadoEntrega(idDonacion, actualizacion);
    }

    /** Chofer autenticado. Prioriza email si logística lo expone; si no, nombre/usuario. */
    public Optional<ChoferDTO> choferActual(Authentication authentication) {
        List<ChoferDTO> choferes = logisticasClient.obtenerChoferes().stream()
                .filter(ChoferDTO::estaActivo)
                .toList();
        if (choferes.isEmpty()) {
            return Optional.empty();
        }

        String emailCuenta = emailDe(authentication);
        if (emailCuenta != null) {
            for (ChoferDTO chofer : choferes) {
                if (chofer.getEmail() != null && chofer.getEmail().equalsIgnoreCase(emailCuenta)) {
                    return Optional.of(chofer);
                }
            }
        }

        List<String> claves = clavesDe(authentication);
        for (ChoferDTO chofer : choferes) {
            String nombre = normalizar(chofer.getNombre());
            if (nombre.isBlank()) continue;
            for (String clave : claves) {
                if (nombre.equals(clave) || nombre.contains(clave) || clave.contains(nombre)) {
                    return Optional.of(chofer);
                }
            }
        }

        return choferes.size() == 1 ? Optional.of(choferes.get(0)) : Optional.empty();
    }

    private ChoferDTO choferRequerido(Authentication authentication) {
        return choferActual(authentication)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No pudimos identificar tu perfil de chofer. Pedile a administración que cargue tu nombre completo en la flota."));
    }

    private LogisticaDTOs.Camion camionDe(ChoferDTO chofer, LogisticaDTOs.Ruta rutaActiva) {
        if (rutaActiva != null && rutaActiva.getCamionAsignado() != null) {
            return rutaActiva.getCamionAsignado();
        }
        for (CamionDTO camion : logisticasClient.obtenerCamiones()) {
            if (Objects.equals(chofer.getIdChofer(), camion.getIdChofer())) {
                LogisticaDTOs.Camion asignado = new LogisticaDTOs.Camion();
                asignado.setIdChofer(camion.getIdChofer());
                asignado.setPatente(camion.getPatente());
                asignado.setDisponible(camion.getDisponible());
                return asignado;
            }
        }
        return null;
    }

    private void construirParadas(ChoferDashboardDTO dashboard, LogisticaDTOs.Ruta ruta) {
        List<ChoferDashboardDTO.ParadaVista> paradas = new ArrayList<>();
        boolean actualAsignada = false;
        int totalEntregas = 0;
        int pendientes = 0;

        List<LogisticaDTOs.Parada> paradasRuta = ruta.getParadas() != null ? ruta.getParadas() : List.of();
        for (int i = 0; i < paradasRuta.size(); i++) {
            LogisticaDTOs.Parada parada = paradasRuta.get(i);
            ChoferDashboardDTO.ParadaVista vista = new ChoferDashboardDTO.ParadaVista();
            vista.setNumero(i + 1);
            vista.setDireccion(parada.getDireccion());

            List<ChoferDashboardDTO.BienVista> bienes = bienesDe(parada);
            int entregadas = (int) bienes.stream().filter(ChoferDashboardDTO.BienVista::isEntregada).count();
            vista.setBienes(bienes.stream()
                    .sorted(Comparator.comparing(ChoferDashboardDTO.BienVista::isEntregada))
                    .toList());
            vista.setEntregadas(entregadas);
            vista.setTotal(bienes.size());
            vista.setCompleta(entregadas == bienes.size());
            vista.setActual(!vista.isCompleta() && !actualAsignada);
            if (vista.isActual()) {
                actualAsignada = true;
            }

            totalEntregas += bienes.size();
            pendientes += bienes.size() - entregadas;
            paradas.add(vista);
        }

        dashboard.setParadas(paradas);
        dashboard.setEntregasTotales(totalEntregas);
        dashboard.setEntregasPendientes(pendientes);
    }

    private void construirDeposito(ChoferDashboardDTO dashboard, LogisticaDTOs.Ruta ruta) {
        LogisticaDTOs.Deposito deposito = ruta.getDeposito();
        if (deposito == null || !deposito.tieneCoordenadas()) {
            return;
        }
        LogisticaDTOs.DireccionLogistica direccion = new LogisticaDTOs.DireccionLogistica();
        direccion.setCalleUno(deposito.getEtiqueta() != null && !deposito.getEtiqueta().isBlank()
                ? deposito.getEtiqueta() : "Depósito");
        direccion.setLatitud(deposito.getLatitud());
        direccion.setLongitud(deposito.getLongitud());
        ChoferDashboardDTO.DepositoVista vista = new ChoferDashboardDTO.DepositoVista();
        vista.setEtiqueta(direccion.getCalleUno());
        vista.setDireccion(direccion);
        dashboard.setDeposito(vista);
    }

    private List<ChoferDashboardDTO.BienVista> bienesDe(LogisticaDTOs.Parada parada) {
        if (parada.getItems() == null || parada.getItems().getBienes() == null) {
            return List.of();
        }
        List<UUID> ids = parada.getItems().getIdsDonaciones() != null
                ? parada.getItems().getIdsDonaciones() : List.of();
        List<ChoferDashboardDTO.BienVista> bienes = new ArrayList<>();
        List<LogisticaDTOs.Bien> items = parada.getItems().getBienes();
        for (int i = 0; i < items.size(); i++) {
            LogisticaDTOs.Bien item = items.get(i);
            ChoferDashboardDTO.BienVista vista = new ChoferDashboardDTO.BienVista();
            vista.setIdDonacion(i < ids.size() ? ids.get(i) : null);
            vista.setCantidad(item.getCantidad());
            vista.setUnidad(item.getUnidadDeMedida());
            vista.setEstado(item.getEstado());
            vista.setEntregada("ENTREGADA".equalsIgnoreCase(item.getEstado()));
            vista.setEstadoEtiqueta(etiquetaEstado(item.getEstado()));
            bienes.add(vista);
        }
        return bienes;
    }

    private void construirGeojson(ChoferDashboardDTO dashboard) {
        List<List<Double>> coordenadas = new ArrayList<>();
        List<Map<String, Object>> paradas = new ArrayList<>();

        LogisticaDTOs.Deposito deposito = dashboard.getRutaActiva() != null
                ? dashboard.getRutaActiva().getDeposito() : null;
        if (deposito != null && deposito.tieneCoordenadas()) {
            coordenadas.add(List.of(deposito.getLongitud(), deposito.getLatitud()));
            Map<String, Object> puntoDeposito = new LinkedHashMap<>();
            puntoDeposito.put("parada", 0);
            puntoDeposito.put("estado", "deposito");
            puntoDeposito.put("direccion", deposito.getEtiqueta() != null ? deposito.getEtiqueta() : "Depósito");
            puntoDeposito.put("entregadas", 0);
            puntoDeposito.put("total", 0);
            paradas.add(puntoDeposito);
        }

        for (ChoferDashboardDTO.ParadaVista parada : dashboard.getParadas()) {
            LogisticaDTOs.DireccionLogistica direccion = parada.getDireccion();
            if (direccion == null || direccion.getLatitud() == null || direccion.getLongitud() == null) {
                continue;
            }
            coordenadas.add(List.of(direccion.getLongitud(), direccion.getLatitud()));
            Map<String, Object> punto = new LinkedHashMap<>();
            punto.put("parada", parada.getNumero());
            punto.put("estado", parada.isActual() ? "actual" : (parada.isCompleta() ? "completa" : "pendiente"));
            punto.put("direccion", direccion.etiqueta());
            punto.put("entregadas", parada.getEntregadas());
            punto.put("total", parada.getTotal());
            paradas.add(punto);
        }

        List<Map<String, Object>> features = new ArrayList<>();
        if (coordenadas.size() >= 2) {
            features.add(feature("LineString", coordenadas, null));
        }
        for (int i = 0; i < coordenadas.size(); i++) {
            features.add(feature("Point", coordenadas.get(i), paradas.get(i)));
        }

        Map<String, Object> coleccion = new LinkedHashMap<>();
        coleccion.put("type", "FeatureCollection");
        coleccion.put("features", features);
        try {
            dashboard.setGeojson(objectMapper.writeValueAsString(coleccion));
        } catch (Exception e) {
            dashboard.setGeojson("{\"type\":\"FeatureCollection\",\"features\":[]}");
        }
        dashboard.setHayTrazado(!coordenadas.isEmpty());
    }

    private Map<String, Object> feature(String tipo, Object coordenadas, Map<String, Object> propiedades) {
        Map<String, Object> geometria = new LinkedHashMap<>();
        geometria.put("type", tipo);
        geometria.put("coordinates", coordenadas);

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometria);
        feature.put("properties", propiedades != null ? propiedades : Map.of());
        return feature;
    }

    /** La entrega pertenece a la primera parada con paquetes sin entregar. */
    private static boolean esParadaActual(LogisticaDTOs.Ruta ruta, UUID idDonacion) {
        if (ruta.getParadas() == null) return false;
        for (LogisticaDTOs.Parada parada : ruta.getParadas()) {
            if (parada.getItems() == null || parada.getItems().getBienes() == null) {
                continue;
            }
            boolean completa = parada.getItems().getBienes().stream()
                    .allMatch(item -> "ENTREGADA".equalsIgnoreCase(item.getEstado()));
            if (completa) {
                continue;
            }
            return parada.getItems().getIdsDonaciones() != null
                    && parada.getItems().getIdsDonaciones().contains(idDonacion);
        }
        return false;
    }

    private static boolean rutaContiene(LogisticaDTOs.Ruta ruta, UUID idDonacion) {
        if (ruta.getParadas() == null) return false;
        return ruta.getParadas().stream()
                .filter(parada -> parada.getItems() != null)
                .filter(parada -> parada.getItems().getIdsDonaciones() != null)
                .flatMap(parada -> parada.getItems().getIdsDonaciones().stream())
                .anyMatch(idDonacion::equals);
    }

    private static String etiquetaEstado(String estado) {
        if (estado == null) return "Pendiente";
        return switch (estado.toUpperCase(Locale.ROOT)) {
            case "PENDIENTE" -> "Pendiente";
            case "EN_CAMINO" -> "En camino";
            case "ENTREGADA" -> "Entregada";
            case "NO_RECIBIDA" -> "No recibida";
            default -> estado;
        };
    }

    private static String nombreVisible(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String nombre = oidcUser.getFullName();
            if (nombre != null && !nombre.isBlank()) return nombre;
        }
        return authentication != null ? authentication.getName() : "";
    }

    private static String emailDe(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getEmail();
        }
        return null;
    }

    private static List<String> clavesDe(Authentication authentication) {
        Set<String> claves = new LinkedHashSet<>();
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            agregar(claves, oidcUser.getFullName());
            agregar(claves, unir(oidcUser.getGivenName(), oidcUser.getFamilyName()));
            agregar(claves, oidcUser.getPreferredUsername());
            agregar(claves, oidcUser.getEmail() != null ? oidcUser.getEmail().split("@")[0] : null);
        }
        agregar(claves, authentication != null ? authentication.getName() : null);
        return claves.stream().filter(clave -> clave.length() >= 3).toList();
    }

    private static void agregar(Set<String> claves, String valor) {
        if (valor == null) return;
        String normalizado = normalizar(valor);
        if (!normalizado.isBlank()) {
            claves.add(normalizado);
        }
    }

    private static String unir(String a, String b) {
        if (a == null && b == null) return null;
        return ((a != null ? a : "") + " " + (b != null ? b : "")).trim();
    }

    private static String normalizar(String valor) {
        if (valor == null) return "";
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
