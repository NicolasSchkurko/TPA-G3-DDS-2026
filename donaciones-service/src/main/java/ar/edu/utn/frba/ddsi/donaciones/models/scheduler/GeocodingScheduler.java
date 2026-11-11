package ar.edu.utn.frba.ddsi.donaciones.models.scheduler;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorGeoref;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Backfill nocturno de coordenadas (Punto 75).
 *
 * <p>No reemplaza el geocoding del alta: solo reintenta lo que quedó nulo porque Georef estaba
 * caído o no encontró la dirección en ese momento. Corre a las 00:30 para no competir con
 * {@code ActividadDonanteScheduler} ni {@code AsignacionScheduler}, que arrancan a las 00:00
 * sobre el scheduler de un solo hilo de Spring.
 *
 * <p>No lleva {@code @Transactional} sobre todo el método a propósito: cada lote se persiste
 * por separado (Punto 74). Una fila que falle no revierte las anteriores.
 */
@Component
public class GeocodingScheduler {

    private static final Logger log = LoggerFactory.getLogger(GeocodingScheduler.class);

    /** Tope de filas leídas por iteración; el gestor igual parte en lotes de 1000. */
    private static final int TAMANO_LOTE = GestorGeoref.TAMANO_LOTE;

    private final GestorGeoref gestorGeoref;
    private final boolean habilitado;
    private final int maxPorCorrida;
    private final int reintentoDias;
    private final int intentosMaximos;

    public GeocodingScheduler(
            GestorGeoref gestorGeoref,
            @Value("${geocoding.cron.enabled:true}") boolean habilitado,
            @Value("${geocoding.cron.max-por-corrida:1000}") int maxPorCorrida,
            @Value("${geocoding.cron.reintento-dias:7}") int reintentoDias,
            @Value("${geocoding.cron.intentos-maximos:3}") int intentosMaximos) {
        this.gestorGeoref = gestorGeoref;
        this.habilitado = habilitado;
        this.maxPorCorrida = maxPorCorrida;
        this.reintentoDias = reintentoDias;
        this.intentosMaximos = intentosMaximos;
    }

    @Scheduled(cron = "${geocoding.cron.cron:0 30 0 * * ?}")
    public void geocodificarPendientes() {
        if (!habilitado || !gestorGeoref.isEnabled() || maxPorCorrida <= 0) {
            return;
        }

        LocalDateTime limite = LocalDateTime.now().minusDays(reintentoDias);
        int restantes = maxPorCorrida;
        int procesadas = 0;

        while (restantes > 0) {
            int tamano = Math.min(TAMANO_LOTE, restantes);
            List<Direccion> lote = gestorGeoref.buscarPendientes(limite, intentosMaximos, tamano);

            if (lote.isEmpty()) {
                break;
            }

            // Si Georef falla, se guardan igual las coordenadas parciales que sí se resolvieron
            // antes del corte; las nulas no se marcan como intentadas y se reintentan mañana.
            boolean georefOk = gestorGeoref.completarCoordenadas(lote);
            gestorGeoref.guardarResultados(lote, georefOk);
            procesadas += lote.size();

            if (!georefOk) {
                log.warn("Cron de geocoding interrumpido por fallo de Georef tras {} direcciones",
                        procesadas);
                return;
            }

            restantes -= lote.size();
        }

        if (procesadas > 0) {
            log.info("Cron de geocoding procesó {} direcciones pendientes", procesadas);
        }
    }
}
