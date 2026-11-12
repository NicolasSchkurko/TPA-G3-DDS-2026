package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Consolida el seguimiento de donantes y entidades. La ubicación del camión no es GPS:
 * se calcula a partir de la parada actual de la ruta (primera parada con paquetes sin
 * entregar) y del depósito fijo configurado en logística.
 */
@Service
public class SeguimientoService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** Fallback si logística no devuelve el depósito configurado. */
    private static final double[] DEPOSITO = {-34.5986317, -58.4226973};

    /** Coordenadas aproximadas de barrios/zonas de CABA para el geocoding demo. */
    private static final Map<String, double[]> ZONAS_CABA = Map.ofEntries(
            Map.entry("palermo", new double[]{-34.5875, -58.4200}),
            Map.entry("belgrano", new double[]{-34.5628, -58.4560}),
            Map.entry("recoleta", new double[]{-34.5875, -58.3973}),
            Map.entry("balvanera", new double[]{-34.6090, -58.4030}),
            Map.entry("almagro", new double[]{-34.6099, -58.4200}),
            Map.entry("caballito", new double[]{-34.6186, -58.4466}),
            Map.entry("flores", new double[]{-34.6285, -58.4631}),
            Map.entry("liniers", new double[]{-34.6420, -58.5230}),
            Map.entry("barracas", new double[]{-34.6375, -58.3816}),
            Map.entry("la boca", new double[]{-34.6345, -58.3660}),
            Map.entry("villa soldati", new double[]{-34.6538, -58.4478}),
            Map.entry("villa urquiza", new double[]{-34.5764, -58.4800}),
            Map.entry("san isidro", new double[]{-34.4729, -58.5023}),
            Map.entry("moron", new double[]{-34.6534, -58.6198}),
            Map.entry("san miguel", new double[]{-34.5433, -58.7148}),
            Map.entry("mataderos", new double[]{-34.6596, -58.5030})
    );

    private static final double[] DEFAULT_ZONE = {-34.6200, -58.4300};
    private static final Set<String> ESTADOS_ACTIVOS = Set.of("ASIGNADO", "LISTA_PARA_ENTREGAR", "EN_TRASLADO");

    private final LogisticasClient logisticasClient;

    public SeguimientoService(LogisticasClient logisticasClient) {
        this.logisticasClient = logisticasClient;
    }

    /** Construye la vista de seguimiento para la entidad indicada. */
    public SeguimientoDTO construir(UUID idEntidad, List<DonacionDTO> donacionesEntidad) {
        SeguimientoDTO dto = new SeguimientoDTO();
        dto.setUbicacionSimulada(false);
        dto.setGeneradoEn(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));

        List<LogisticaDTOs.Ruta> rutas = logisticasClient.obtenerRutasActivas();
        aplicarDeposito(dto, rutas);

        List<SeguimientoDTO.EntregaActiva> activas = entregasActivas(idEntidad);
        dto.setEntregas(activas);
        dto.setCamiones(camionesPara(idEntidad, activas, rutas));
        return dto;
    }

    /** Seguimiento para el mapa del donante: camiones de las rutas con sus donaciones activas. */
    public SeguimientoDTO construirDonante(List<DonacionDTO> donaciones) {
        SeguimientoDTO dto = new SeguimientoDTO();
        dto.setUbicacionSimulada(false);
        dto.setGeneradoEn(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));

        Set<UUID> activas = new LinkedHashSet<>();
        if (donaciones != null) {
            donaciones.stream()
                    .filter(donacion -> donacion.getId() != null)
                    .filter(donacion -> ESTADOS_ACTIVOS.contains(normalizarEstado(donacion.getEstado())))
                    .forEach(donacion -> activas.add(donacion.getId()));
        }

        List<LogisticaDTOs.Ruta> rutas = logisticasClient.obtenerRutasActivas();
        aplicarDeposito(dto, rutas);

        List<SeguimientoDTO.CamionEnRuta> camiones = new ArrayList<>();
        List<SeguimientoDTO.EntregaActiva> entregas = new ArrayList<>();
        for (LogisticaDTOs.Ruta ruta : rutas) {
            List<LogisticaDTOs.Parada> paradas = ruta.getParadas() != null ? ruta.getParadas() : List.of();
            List<UUID> donacionesDeRuta = new ArrayList<>();
            for (LogisticaDTOs.Parada parada : paradas) {
                if (parada.getItems() == null || parada.getItems().getIdsDonaciones() == null) continue;
                for (UUID id : parada.getItems().getIdsDonaciones()) {
                    if (activas.contains(id) && !estaEntregada(parada, id)) {
                        donacionesDeRuta.add(id);
                        entregas.add(entregaDeDonante(id, parada));
                    }
                }
            }
            if (donacionesDeRuta.isEmpty()) continue;
            camiones.add(camionDeRuta(ruta, donacionesDeRuta));
        }

        dto.setCamiones(camiones);
        dto.setEntregas(entregas);
        return dto;
    }

    /** Mapa donacionId -> entrega (estado y fecha) para la bandeja de notificaciones y el detalle. */
    public Map<UUID, LogisticaDTOs.Bien> entregasPorDonacion() {
        Map<UUID, LogisticaDTOs.Bien> resultado = new LinkedHashMap<>();
        LogisticaDTOs.Bienes bienes = logisticasClient.obtenerBienes();
        if (bienes == null || bienes.getBienes() == null) {
            return resultado;
        }
        List<UUID> ids = bienes.getIdsDonaciones() != null ? bienes.getIdsDonaciones() : List.of();
        List<LogisticaDTOs.Bien> lista = bienes.getBienes();
        for (int i = 0; i < lista.size(); i++) {
            UUID id = i < ids.size() ? ids.get(i) : null;
            if (id != null) resultado.put(id, lista.get(i));
        }
        return resultado;
    }

    private List<SeguimientoDTO.EntregaActiva> entregasActivas(UUID idEntidad) {
        List<SeguimientoDTO.EntregaActiva> activas = new ArrayList<>();
        LogisticaDTOs.Bienes bienes = logisticasClient.obtenerBienes();
        if (bienes == null || bienes.getBienes() == null) {
            return activas;
        }
        List<UUID> ids = bienes.getIdsDonaciones() != null ? bienes.getIdsDonaciones() : List.of();
        List<LogisticaDTOs.Bien> lista = bienes.getBienes();
        for (int i = 0; i < lista.size(); i++) {
            LogisticaDTOs.Bien bien = lista.get(i);
            if (!idEntidad.equals(bien.idEntidadDestino()) || !bien.estadoActivo()) continue;
            String zona = zonaDe(bien.getEntidadDestino());
            SeguimientoDTO.EntregaActiva activa = new SeguimientoDTO.EntregaActiva();
            activa.setIdDonacion(i < ids.size() ? ids.get(i) : null);
            activa.setEstado(bien.getEstado());
            activa.setCantidad(bien.getCantidad());
            activa.setUnidadDeMedida(bien.getUnidadDeMedida());
            activa.setDestino(bien.getEntidadDestino() != null ? bien.getEntidadDestino().etiqueta() : null);
            activa.setZona(zona);
            double[] destino = coordenadasDe(bien.getEntidadDestino(), zona);
            activa.setLat(destino[0]);
            activa.setLng(destino[1]);
            activas.add(activa);
        }
        return activas;
    }

    private List<SeguimientoDTO.CamionEnRuta> camionesPara(UUID idEntidad,
                                                           List<SeguimientoDTO.EntregaActiva> activas,
                                                           List<LogisticaDTOs.Ruta> rutas) {
        List<SeguimientoDTO.CamionEnRuta> camiones = new ArrayList<>();
        for (LogisticaDTOs.Ruta ruta : rutas) {
            if (!ruta.contieneEntidad(idEntidad)) continue;
            List<UUID> donacionesDeEstaEntidad = new ArrayList<>();
            for (LogisticaDTOs.Parada parada : ruta.getParadas() != null ? ruta.getParadas() : List.<LogisticaDTOs.Parada>of()) {
                if (idEntidad.equals(parada.idEntidadDestino()) && parada.getItems() != null
                        && parada.getItems().getIdsDonaciones() != null) {
                    donacionesDeEstaEntidad.addAll(parada.getItems().getIdsDonaciones());
                }
            }
            camiones.add(camionDeRuta(ruta, donacionesDeEstaEntidad));
        }

        // Sin rutas reales pero con entregas activas: se mantiene un camión por entrega para no
        // dejar el mapa vacío en modo demo.
        if (camiones.isEmpty()) {
            for (SeguimientoDTO.EntregaActiva entrega : activas) {
                double[] destino = centroDe(entrega.getZona());
                String semilla = entrega.getIdDonacion() != null ? entrega.getIdDonacion().toString() : "entrega";
                SeguimientoDTO.CamionEnRuta camion = new SeguimientoDTO.CamionEnRuta();
                camion.setPatente(patenteSimulada(semilla));
                camion.setEstadoRuta("EN_CURSO");
                camion.setDonaciones(entrega.getIdDonacion() != null ? List.of(entrega.getIdDonacion()) : List.of());
                camion.setLat(destino[0]);
                camion.setLng(destino[1]);
                camion.setParadaActual(0);
                camion.setDestino("Destino de la entrega");
                camion.setTrazado(List.of(List.of(DEPOSITO[1], DEPOSITO[0]), List.of(destino[1], destino[0])));
                camion.setUltimaActualizacion(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));
                camion.setEtaMinutos(8 + ThreadLocalRandom.current().nextInt(25));
                camiones.add(camion);
            }
        }
        return camiones;
    }

    private SeguimientoDTO.CamionEnRuta camionDeRuta(LogisticaDTOs.Ruta ruta, List<UUID> donaciones) {
        SeguimientoDTO.CamionEnRuta camion = new SeguimientoDTO.CamionEnRuta();
        camion.setPatente(ruta.patente());
        camion.setChofer(ruta.getCamionAsignado() != null && ruta.getCamionAsignado().getIdChofer() != null
                ? "Chofer " + ruta.getCamionAsignado().getIdChofer().toString().substring(0, 8)
                : null);
        camion.setEstadoRuta(ruta.getEstado());
        camion.setUrlSeguimiento(ruta.getUrlSeguimiento());
        camion.setDonaciones(donaciones);
        camion.setTrazado(trazadoDe(ruta));

        int indice = indiceParadaActual(ruta);
        camion.setParadaActual(indice);
        LogisticaDTOs.Parada parada = indice > 0 && ruta.getParadas() != null
                ? ruta.getParadas().get(indice - 1) : null;
        double[] posicion = parada != null && parada.getDireccion() != null
                && parada.getDireccion().getLatitud() != null
                && parada.getDireccion().getLongitud() != null
                ? coordenadasDe(parada.getDireccion(), null)
                : depositoDe(ruta);
        camion.setLat(posicion[0]);
        camion.setLng(posicion[1]);
        camion.setDestino(parada != null && parada.getDireccion() != null
                ? parada.getDireccion().etiqueta()
                : "Depósito");
        camion.setUltimaActualizacion(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));
        camion.setEtaMinutos(indice > 0 ? 8 + ThreadLocalRandom.current().nextInt(25) : null);
        return camion;
    }

    private static boolean estaEntregada(LogisticaDTOs.Parada parada, UUID idDonacion) {
        if (parada.getItems() == null || parada.getItems().getBienes() == null
                || parada.getItems().getIdsDonaciones() == null) {
            return false;
        }
        List<UUID> ids = parada.getItems().getIdsDonaciones();
        List<LogisticaDTOs.Bien> bienes = parada.getItems().getBienes();
        for (int i = 0; i < bienes.size(); i++) {
            if (i < ids.size() && idDonacion.equals(ids.get(i))) {
                return "ENTREGADA".equalsIgnoreCase(bienes.get(i).getEstado());
            }
        }
        return false;
    }

    private SeguimientoDTO.EntregaActiva entregaDeDonante(UUID idDonacion, LogisticaDTOs.Parada parada) {
        LogisticaDTOs.Bien bien = null;
        List<UUID> ids = parada.getItems().getIdsDonaciones() != null ? parada.getItems().getIdsDonaciones() : List.of();
        List<LogisticaDTOs.Bien> lista = parada.getItems().getBienes() != null ? parada.getItems().getBienes() : List.of();
        for (int i = 0; i < lista.size(); i++) {
            if (i < ids.size() && idDonacion.equals(ids.get(i))) {
                bien = lista.get(i);
                break;
            }
        }

        SeguimientoDTO.EntregaActiva entrega = new SeguimientoDTO.EntregaActiva();
        entrega.setIdDonacion(idDonacion);
        entrega.setEstado(bien != null ? bien.getEstado() : "PENDIENTE");
        entrega.setCantidad(bien != null ? bien.getCantidad() : null);
        entrega.setUnidadDeMedida(bien != null ? bien.getUnidadDeMedida() : null);
        entrega.setDestino(parada.getDireccion() != null ? parada.getDireccion().etiqueta() : null);
        entrega.setZona(zonaDe(parada.getDireccion()));
        double[] coords = coordenadasDe(parada.getDireccion(), entrega.getZona());
        entrega.setLat(coords[0]);
        entrega.setLng(coords[1]);
        return entrega;
    }

    /** Índice 1-based de la primera parada con paquetes sin entregar; 0 si el camión está en depósito. */
    private static int indiceParadaActual(LogisticaDTOs.Ruta ruta) {
        if (ruta.getEstado() == null || "PROGRAMADA".equalsIgnoreCase(ruta.getEstado())) {
            return 0;
        }
        List<LogisticaDTOs.Parada> paradas = ruta.getParadas() != null ? ruta.getParadas() : List.of();
        for (int i = 0; i < paradas.size(); i++) {
            LogisticaDTOs.Bienes items = paradas.get(i).getItems();
            if (items == null || items.getBienes() == null || items.getBienes().isEmpty()) continue;
            boolean pendiente = items.getBienes().stream()
                    .anyMatch(bien -> !"ENTREGADA".equalsIgnoreCase(bien.getEstado()));
            if (pendiente) return i + 1;
        }
        return 0;
    }

    private static List<List<Double>> trazadoDe(LogisticaDTOs.Ruta ruta) {
        List<List<Double>> puntos = new ArrayList<>();
        double[] deposito = depositoDe(ruta);
        puntos.add(List.of(deposito[1], deposito[0]));
        if (ruta.getParadas() != null) {
            for (LogisticaDTOs.Parada parada : ruta.getParadas()) {
                LogisticaDTOs.DireccionLogistica direccion = parada.getDireccion();
                if (direccion == null || direccion.getLatitud() == null || direccion.getLongitud() == null) {
                    continue;
                }
                puntos.add(List.of(direccion.getLongitud(), direccion.getLatitud()));
            }
        }
        return puntos;
    }

    private static double[] depositoDe(LogisticaDTOs.Ruta ruta) {
        if (ruta != null && ruta.getDeposito() != null && ruta.getDeposito().tieneCoordenadas()) {
            return new double[]{ruta.getDeposito().getLatitud(), ruta.getDeposito().getLongitud()};
        }
        return DEPOSITO;
    }

    private static void aplicarDeposito(SeguimientoDTO dto, List<LogisticaDTOs.Ruta> rutas) {
        LogisticaDTOs.Deposito deposito = rutas.stream()
                .map(LogisticaDTOs.Ruta::getDeposito)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        dto.setDepositoLat(deposito != null ? deposito.getLatitud() : DEPOSITO[0]);
        dto.setDepositoLng(deposito != null ? deposito.getLongitud() : DEPOSITO[1]);
        dto.setDepositoEtiqueta(deposito != null ? deposito.getEtiqueta() : "Depósito");
    }

    private static double[] coordenadasDe(LogisticaDTOs.DireccionLogistica direccion, String zona) {
        if (direccion != null && direccion.getLatitud() != null && direccion.getLongitud() != null) {
            return new double[]{direccion.getLatitud(), direccion.getLongitud()};
        }
        return centroDe(zona);
    }

    private static String normalizarEstado(String estado) {
        return estado != null ? estado.toUpperCase(java.util.Locale.ROOT) : "";
    }

    private static String patenteSimulada(String semilla) {
        char a = (char) ('A' + Math.floorMod(semilla.hashCode(), 26));
        char b = (char) ('A' + Math.floorMod(semilla.hashCode() / 26, 26));
        int n1 = Math.floorMod(semilla.hashCode() / 676, 1000);
        char c = (char) ('A' + Math.floorMod(semilla.hashCode() / 676000, 26));
        char d = (char) ('A' + Math.floorMod(semilla.hashCode() / 676000 / 26, 26));
        return String.format("%c%c%03d%c%c", a, b, n1, c, d);
    }

    private static String zonaDe(LogisticaDTOs.DireccionLogistica direccion) {
        if (direccion == null) return "CABA";
        String ciudad = direccion.getCiudad();
        return ciudad != null && !ciudad.isBlank() ? ciudad : "CABA";
    }

    /** Coordenadas del centro de una zona (barrio) conocidas; si no, centro de CABA. */
    public static double[] centroDe(String zona) {
        if (zona == null) return DEFAULT_ZONE;
        String key = zona.toLowerCase();
        double[] found = ZONAS_CABA.get(key);
        if (found != null) return found;
        for (Map.Entry<String, double[]> entry : ZONAS_CABA.entrySet()) {
            if (key.contains(entry.getKey())) return entry.getValue();
        }
        return DEFAULT_ZONE;
    }
}
