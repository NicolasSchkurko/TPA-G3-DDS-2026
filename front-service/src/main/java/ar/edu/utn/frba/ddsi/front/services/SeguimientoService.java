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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Consolida el estado de las entregas activas de una entidad y les asigna una posición
 * en el mapa.
 *
 * <p><strong>Importante:</strong> logisticas-service no expone coordenadas ni GPS de los camiones
 * (solo ciudad de destino y urlSeguimiento). Hasta que exista un endpoint de ubicación, las
 * posiciones se <em>simulan</em> de forma determinista a partir de la zona de destino y se
 * marcan como "Ubicación simulada" en la UI. Cuando el backend exponga lat/lng, reemplazar
 * {@link #posicionSimulada} y mantener el resto del flujo.
 */
@Service
public class SeguimientoService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /** Depósito ficticio de la red (CABA). */
    private static final double[] DEPOSITO = {-34.6500, -58.4920};

    /** Coordenadas aproximadas de barrios/zonas de CABA para el geocoding demo. */
    private static final Map<String, double[]> ZONAS_CABA = Map.ofEntries(
            Map.entry("palermo", new double[]{-34.5875, -58.4200}),
            Map.entry("belgrano", new double[]{-34.5628, -58.4560}),
            Map.entry("recoleta", new double[]{-34.5875, -58.3973}),
            Map.entry("balvanera", new double[]{-34.6090, -58.4030}),
            Map.entry("Almagro".toLowerCase(), new double[]{-34.6099, -58.4200}),
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

    private final LogisticasClient logisticasClient;

    public SeguimientoService(LogisticasClient logisticasClient) {
        this.logisticasClient = logisticasClient;
    }

    /** Construye la vista de seguimiento para la entidad indicada. */
    public SeguimientoDTO construir(UUID idEntidad, List<DonacionDTO> donacionesEntidad) {
        SeguimientoDTO dto = new SeguimientoDTO();
        dto.setUbicacionSimulada(true);
        dto.setGeneradoEn(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));

        List<SeguimientoDTO.EntregaActiva> activas = entregasActivas(idEntidad);
        dto.setEntregas(activas);

        List<SeguimientoDTO.CamionEnRuta> camiones = camionesPara(idEntidad, activas);
        dto.setCamiones(camiones);

        return dto;
    }

    /** Mapa donacionId -> entrega (estado y fecha) para la bandeja de notificaciones y el detalle. */
    public java.util.Map<UUID, LogisticaDTOs.Bien> entregasPorDonacion() {
        java.util.Map<UUID, LogisticaDTOs.Bien> resultado = new java.util.LinkedHashMap<>();
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
            double[] destino = centroDe(zona);
            activa.setLat(destino[0]);
            activa.setLng(destino[1]);
            activas.add(activa);
        }
        return activas;
    }

    private List<SeguimientoDTO.CamionEnRuta> camionesPara(UUID idEntidad, List<SeguimientoDTO.EntregaActiva> activas) {
        List<SeguimientoDTO.CamionEnRuta> camiones = new ArrayList<>();
        List<LogisticaDTOs.Ruta> rutas = logisticasClient.obtenerRutas();
        for (LogisticaDTOs.Ruta ruta : rutas) {
            if (!ruta.contieneEntidad(idEntidad)) continue;
            if (ruta.getEstado() != null && ruta.getEstado().equalsIgnoreCase("FINALIZADA")) continue;
            List<UUID> donacionesDeEstaEntidad = new ArrayList<>();
            if (ruta.getParadas() != null) {
                for (LogisticaDTOs.Parada parada : ruta.getParadas()) {
                    if (idEntidad.equals(parada.idEntidadDestino()) && parada.getItems() != null
                            && parada.getItems().getIdsDonaciones() != null) {
                        donacionesDeEstaEntidad.addAll(parada.getItems().getIdsDonaciones());
                    }
                }
            }
            SeguimientoDTO.CamionEnRuta camion = new SeguimientoDTO.CamionEnRuta();
            camion.setPatente(ruta.patente());
            camion.setChofer(ruta.getCamionAsignado() != null && ruta.getCamionAsignado().getIdChofer() != null
                    ? "Chofer " + ruta.getCamionAsignado().getIdChofer().toString().substring(0, 8)
                    : null);
            camion.setEstadoRuta(ruta.getEstado());
            camion.setUrlSeguimiento(ruta.getUrlSeguimiento());
            camion.setDonaciones(donacionesDeEstaEntidad);
            String zona = activas.stream().findFirst().map(SeguimientoDTO.EntregaActiva::getZona).orElse("CABA");
            double[] destino = centroDe(zona);
            double[] posicion = posicionSimulada(ruta.getIdRuta() != null ? ruta.getIdRuta().toString() : ruta.patente(), destino);
            camion.setLat(posicion[0]);
            camion.setLng(posicion[1]);
            camion.setUltimaActualizacion(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));
            camion.setEtaMinutos(8 + ThreadLocalRandom.current().nextInt(25));
            camiones.add(camion);
        }
        // Sin rutas reales pero con entregas activas: un camión simulado por entrega para poder maquetar el flujo.
        if (camiones.isEmpty()) {
            int n = 0;
            for (SeguimientoDTO.EntregaActiva entrega : activas) {
                double[] destino = centroDe(entrega.getZona());
                String semilla = entrega.getIdDonacion() != null ? entrega.getIdDonacion().toString() : ("entrega-" + n);
                double[] posicion = posicionSimulada(semilla, destino);
                SeguimientoDTO.CamionEnRuta camion = new SeguimientoDTO.CamionEnRuta();
                camion.setPatente(patenteSimulada(semilla));
                camion.setEstadoRuta("EN_CURSO");
                camion.setDonaciones(entrega.getIdDonacion() != null ? List.of(entrega.getIdDonacion()) : List.of());
                camion.setLat(posicion[0]);
                camion.setLng(posicion[1]);
                camion.setUltimaActualizacion(ISO.format(OffsetDateTime.now(ZoneOffset.ofHours(-3))));
                camion.setEtaMinutos(8 + ThreadLocalRandom.current().nextInt(25));
                camiones.add(camion);
                n++;
            }
        }
        return camiones;
    }

    /** Posición simulada entre el depósito y el destino, determinista por semilla. */
    private static double[] posicionSimulada(String semilla, double[] destino) {
        double t = 0.45 + 0.35 * pseudoRandom(semilla);
        double lat = DEPOSITO[0] + (destino[0] - DEPOSITO[0]) * t;
        double lng = DEPOSITO[1] + (destino[1] - DEPOSITO[1]) * t;
        // Offset lateral para que no se pisen los marcadores
        double off = (pseudoRandom(semilla + "#off") - 0.5) * 0.012;
        return new double[]{lat + off, lng - off};
    }

    private static double pseudoRandom(String semilla) {
        long hash = 1125899906842597L;
        for (int i = 0; i < semilla.length(); i++) {
            hash = 31 * hash + semilla.charAt(i);
        }
        return (double) (Math.floorMod(hash, 10000)) / 10000.0;
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
