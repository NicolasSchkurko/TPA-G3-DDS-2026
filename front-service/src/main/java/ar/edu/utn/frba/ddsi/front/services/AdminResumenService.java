package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.LogisticasClient;
import ar.edu.utn.frba.ddsi.front.dto.CamionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.logistica.LogisticaDTOs;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminResumenService {

    private final DonacionesClient donacionesClient;
    private final LogisticasClient logisticasClient;

    @Value("${admin.resumen.cache-ms:60000}")
    private long cacheMs;

    private final Object lock = new Object();
    private Resumen cached;
    private long cachedAt;

    public AdminResumenService(DonacionesClient donacionesClient, LogisticasClient logisticasClient) {
        this.donacionesClient = donacionesClient;
        this.logisticasClient = logisticasClient;
    }

    public Resumen obtenerResumen() {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            if (cached != null && (now - cachedAt) < cacheMs) {
                return cached;
            }
            try {
                Resumen nuevo = construir();
                nuevo.setDesactualizado(false);
                cached = nuevo;
                cachedAt = now;
                return nuevo;
            } catch (Exception e) {
                if (cached != null) {
                    Resumen desactualizado = cached.copia();
                    desactualizado.setDesactualizado(true);
                    return desactualizado;
                }
                Resumen vacio = new Resumen();
                vacio.setDesactualizado(true);
                return vacio;
            }
        }
    }

    /** Fuerza recarga (útil para tests). */
    public void invalidarCache() {
        synchronized (lock) {
            cached = null;
            cachedAt = 0;
        }
    }

    private Resumen construir() {
        Resumen r = new Resumen();

        List<DonacionDTO> donaciones = donacionesClient.obtenerTodasLasDonaciones();
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (DonacionDTO d : donaciones) {
            String estado = d.getEstado() == null ? "SIN_ESTADO" : d.getEstado();
            porEstado.merge(estado, 1L, Long::sum);
        }
        r.setDonacionesPorEstado(porEstado);
        r.setTotalDonaciones(donaciones.size());

        r.setDonantes(donacionesClient.obtenerDonantes().size());

        List<EntidadBeneficiariaDTO> entidades = donacionesClient.obtenerEntidades();
        // El listado de entidades no trae las necesidades embebidas: hay que pedirlas aparte
        // para que el resumen del panel admin cuente las activas reales y no siempre cero.
        for (EntidadBeneficiariaDTO entidad : entidades) {
            if (entidad.getNecesidades() == null || entidad.getNecesidades().isEmpty()) {
                entidad.setNecesidades(donacionesClient.obtenerNecesidadesDeEntidad(entidad.getId()));
            }
        }
        r.setEntidades(entidades.size());
        long necesidadesActivas = entidades.stream()
                .flatMap(e -> e.necesidadesActivas().stream())
                .count();
        r.setNecesidadesActivas(necesidadesActivas);

        List<CamionDTO> camiones = logisticasClient.obtenerCamiones();
        r.setCamionesDisponibles(camiones.stream().filter(c -> Boolean.TRUE.equals(c.getDisponible())).count());
        r.setCamionesOcupados(camiones.stream().filter(c -> !Boolean.TRUE.equals(c.getDisponible())).count());

        r.setPendientesMatchmaking(donacionesClient.obtenerResultadosMatchmaking().size());

        LogisticaDTOs.Bienes noRecibidas = logisticasClient.obtenerEntregasNoRecibidas();
        long nr = (noRecibidas == null || noRecibidas.getBienes() == null)
                ? 0 : noRecibidas.getBienes().size();
        r.setEntregasNoRecibidas(nr);

        r.setRutasActivas(logisticasClient.obtenerRutasActivas().size());

        r.setUltimasDonaciones(donaciones.stream().limit(8).toList());

        return r;
    }

    @Getter
    @Setter
    public static class Resumen {
        private Map<String, Long> donacionesPorEstado = new LinkedHashMap<>();
        private long totalDonaciones;
        private long donantes;
        private long entidades;
        private long necesidadesActivas;
        private long camionesDisponibles;
        private long camionesOcupados;
        private long pendientesMatchmaking;
        private long entregasNoRecibidas;
        private long rutasActivas;
        private List<DonacionDTO> ultimasDonaciones = List.of();
        private boolean desactualizado;

        public Resumen copia() {
            Resumen copia = new Resumen();
            copia.setDonacionesPorEstado(new LinkedHashMap<>(donacionesPorEstado));
            copia.setTotalDonaciones(totalDonaciones);
            copia.setDonantes(donantes);
            copia.setEntidades(entidades);
            copia.setNecesidadesActivas(necesidadesActivas);
            copia.setCamionesDisponibles(camionesDisponibles);
            copia.setCamionesOcupados(camionesOcupados);
            copia.setPendientesMatchmaking(pendientesMatchmaking);
            copia.setEntregasNoRecibidas(entregasNoRecibidas);
            copia.setRutasActivas(rutasActivas);
            copia.setUltimasDonaciones(List.copyOf(ultimasDonaciones));
            copia.setDesactualizado(desactualizado);
            return copia;
        }
    }
}
