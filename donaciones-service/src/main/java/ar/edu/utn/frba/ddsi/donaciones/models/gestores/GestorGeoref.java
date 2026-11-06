package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.clients.GeorefClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.GeorefException;
import ar.edu.utn.frba.ddsi.donaciones.dto.georef.GeorefDTOs;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.DireccionJpaRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gestor de geocoding: coordina Georef con las direcciones del dominio.
 *
 * <p>El service de alta/edición solo le pasa la dirección; acá vive la decisión de cuándo
 * consultar, cómo deduplicar y lotear, y cómo persistir el resultado sin frenar la transacción
 * de negocio.
 */
@Service
public class GestorGeoref {

    private static final Logger log = LoggerFactory.getLogger(GestorGeoref.class);

    public static final int TAMANO_LOTE = 1000;

    private final GeorefClient georefClient;
    private final DireccionJpaRepository repositorioDirecciones;
    private final TransactionTemplate transaccion;
    private final boolean habilitado;

    public GestorGeoref(GeorefClient georefClient, boolean habilitado) {
        this.georefClient = georefClient;
        this.repositorioDirecciones = null;
        this.transaccion = null;
        this.habilitado = habilitado;
    }

    @Autowired
    public GestorGeoref(
            GeorefClient georefClient,
            DireccionJpaRepository repositorioDirecciones,
            PlatformTransactionManager transactionManager,
            @Value("${geocoding.enabled:true}") boolean habilitado) {
        this.georefClient = georefClient;
        this.repositorioDirecciones = repositorioDirecciones;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.habilitado = habilitado;
    }

    public boolean isEnabled() {
        return habilitado;
    }

    public void geocodificarAlta(Direccion direccion) {
        programarDespuesDelCommit(direccion);
    }

    /**
     * Si la dirección textual no cambió, conserva coordenadas y backoff para no consultar Georef
     * de nuevo ni reiniciar los reintentos.
     */
    public void geocodificarActualizacion(Direccion nueva, Direccion existente) {
        if (nueva == null || nueva.tieneCoordenadas()) {
            return;
        }
        if (existente != null && existente.mismaDireccion(nueva)) {
            nueva.setLatitud(existente.getLatitud());
            nueva.setLongitud(existente.getLongitud());
            nueva.setIntentosGeocoding(existente.getIntentosGeocoding());
            nueva.setUltimoIntentoGeocoding(existente.getUltimoIntentoGeocoding());
            return;
        }
        programarDespuesDelCommit(nueva);
    }

    /**
     * Geocodifica las direcciones que todavía no tienen coordenadas.
     *
     * @return {@code true} si Georef respondió (aunque no haya encontrado algunas), {@code false}
     *         si hubo una falla de infraestructura.
     */
    public boolean completarCoordenadas(List<Direccion> direcciones) {
        if (!habilitado || direcciones == null || direcciones.isEmpty()) {
            return true;
        }

        Map<String, List<Direccion>> porClave = agruparPendientes(direcciones);
        if (porClave.isEmpty()) {
            return true;
        }

        List<String> claves = new ArrayList<>(porClave.keySet());
        for (int inicio = 0; inicio < claves.size(); inicio += TAMANO_LOTE) {
            List<String> lote = claves.subList(inicio, Math.min(inicio + TAMANO_LOTE, claves.size()));
            if (!geocodificarLote(lote, porClave)) {
                return false;
            }
        }
        return true;
    }

    public boolean completarCoordenadas(Direccion direccion) {
        return direccion == null || completarCoordenadas(List.of(direccion));
    }

    /**
     * Persiste el resultado de un lote del cron en una transacción corta.
     *
     * <p>Vuelve a leer las filas con lock para no pisar coordenadas que otra instancia haya
     * resuelto mientras se llamaba a Georef. Si {@code georefOk} es {@code false}, solo guarda
     * las direcciones resueltas y no consume intentos de las que quedaron nulas.
     */
    public void guardarResultados(List<Direccion> resultados, boolean georefOk) {
        if (!habilitado || repositorioDirecciones == null || transaccion == null
                || resultados == null || resultados.isEmpty()) {
            return;
        }

        transaccion.execute(status -> {
            persistirResultados(resultados, georefOk);
            return null;
        });
    }

    private void persistirResultados(List<Direccion> resultados, boolean georefOk) {
        List<UUID> ids = resultados.stream()
                .map(Direccion::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return;
        }

        Map<UUID, Direccion> actuales = repositorioDirecciones.buscarParaActualizar(ids).stream()
                .collect(Collectors.toMap(Direccion::getId, direccion -> direccion));

        LocalDateTime ahora = LocalDateTime.now();
        for (Direccion resultado : resultados) {
            Direccion actual = actuales.get(resultado.getId());
            if (actual == null || actual.tieneCoordenadas()) {
                continue;
            }

            if (resultado.tieneCoordenadas()) {
                actual.setLatitud(resultado.getLatitud());
                actual.setLongitud(resultado.getLongitud());
                actual.setIntentosGeocoding(0);
                actual.setUltimoIntentoGeocoding(ahora);
            } else if (georefOk && !sinCalle(actual)) {
                actual.setIntentosGeocoding(actual.getIntentosGeocoding() + 1);
                actual.setUltimoIntentoGeocoding(ahora);
            }
        }
    }

    private void programarDespuesDelCommit(Direccion direccion) {
        if (direccion == null || georefClient == null || !habilitado) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    geocodificarYGuardar(direccion);
                }
            });
        } else {
            // Sin transacción (tests o llamadas directas) se completa en memoria y se persiste
            // junto con la entidad.
            geocodificar(direccion);
        }
    }

    private void geocodificarYGuardar(Direccion direccion) {
        try {
            boolean georefOk = completarCoordenadas(direccion);
            guardarResultados(List.of(direccion), georefOk);
        } catch (RuntimeException e) {
            log.warn("No se pudo guardar el geocoding de la dirección {}", direccion.getId(), e);
        }
    }

    private void geocodificar(Direccion direccion) {
        try {
            completarCoordenadas(direccion);
        } catch (RuntimeException e) {
            log.warn("No se pudo geocodificar la dirección {}", direccion.getId(), e);
        }
    }

    private Map<String, List<Direccion>> agruparPendientes(List<Direccion> direcciones) {
        Map<String, List<Direccion>> porClave = new LinkedHashMap<>();
        for (Direccion direccion : direcciones) {
            if (direccion == null || direccion.tieneCoordenadas() || sinCalle(direccion)) {
                continue;
            }
            porClave.computeIfAbsent(clave(direccion), k -> new ArrayList<>()).add(direccion);
        }
        return porClave;
    }

    private boolean geocodificarLote(List<String> claves, Map<String, List<Direccion>> porClave) {
        List<GeorefDTOs.Resultado> resultados;
        try {
            resultados = georefClient.buscar(consultas(claves, porClave, true));
        } catch (GeorefException e) {
            log.warn("No se pudo geocodificar un lote de {} direcciones", claves.size(), e);
            return false;
        }

        aplicar(claves, porClave, resultados);

        List<String> sinResultado = claves.stream()
                .filter(clave -> !porClave.get(clave).get(0).tieneCoordenadas())
                .toList();

        if (!sinResultado.isEmpty()) {
            try {
                aplicar(sinResultado, porClave,
                        georefClient.buscar(consultas(sinResultado, porClave, false)));
            } catch (GeorefException e) {
                log.warn("Georef no respondió en el reintento sin localidad para {} direcciones",
                        sinResultado.size(), e);
                return false;
            }
        }

        long resueltas = claves.stream()
                .filter(clave -> porClave.get(clave).get(0).tieneCoordenadas())
                .count();
        log.debug("Geocoding: {} de {} direcciones resueltas", resueltas, claves.size());
        return true;
    }

    private List<GeorefDTOs.Consulta> consultas(
            List<String> claves,
            Map<String, List<Direccion>> porClave,
            boolean conLocalidad) {
        List<GeorefDTOs.Consulta> consultas = new ArrayList<>(claves.size());
        for (String clave : claves) {
            List<Direccion> direcciones = porClave.get(clave);
            if (direcciones == null || direcciones.isEmpty()) {
                continue;
            }
            consultas.add(consulta(direcciones.get(0), conLocalidad));
        }
        return consultas;
    }

    private GeorefDTOs.Consulta consulta(Direccion direccion, boolean conLocalidad) {
        String calle = direccion.getCalleUno().trim();
        if (direccion.getAltura() != null) {
            calle = calle + " " + direccion.getAltura();
        }

        String provincia = null;
        String localidad = null;
        if (direccion.getCiudad() != null) {
            localidad = blankToNull(direccion.getCiudad().getNombre());
            if (direccion.getCiudad().getProvincia() != null) {
                provincia = blankToNull(direccion.getCiudad().getProvincia().getNombre());
            }
        }

        return GeorefDTOs.Consulta.de(calle, provincia, conLocalidad ? localidad : null);
    }

    private void aplicar(
            List<String> claves,
            Map<String, List<Direccion>> porClave,
            List<GeorefDTOs.Resultado> resultados) {
        if (resultados == null) {
            return;
        }

        for (int i = 0; i < claves.size() && i < resultados.size(); i++) {
            GeorefDTOs.Resultado resultado = resultados.get(i);
            if (resultado == null || resultado.direcciones() == null
                    || resultado.direcciones().isEmpty()) {
                continue;
            }

            GeorefDTOs.Direccion encontrada = resultado.direcciones().get(0);
            if (encontrada == null || encontrada.ubicacion() == null) {
                continue;
            }

            GeorefDTOs.Ubicacion ubicacion = encontrada.ubicacion();
            if (ubicacion.lat() == null || ubicacion.lon() == null) {
                continue;
            }

            List<Direccion> direcciones = porClave.get(claves.get(i));
            if (direcciones == null) {
                continue;
            }
            for (Direccion direccion : direcciones) {
                direccion.setLatitud(ubicacion.lat());
                direccion.setLongitud(ubicacion.lon());
            }
        }
    }

    private String clave(Direccion direccion) {
        String ciudad = direccion.getCiudad() != null ? direccion.getCiudad().getNombre() : null;
        String provincia = direccion.getCiudad() != null
                && direccion.getCiudad().getProvincia() != null
                ? direccion.getCiudad().getProvincia().getNombre()
                : null;
        return normalizar(direccion.getCalleUno()) + "|" + direccion.getAltura()
                + "|" + normalizar(ciudad) + "|" + normalizar(provincia);
    }

    private static boolean sinCalle(Direccion direccion) {
        return direccion.getCalleUno() == null || direccion.getCalleUno().isBlank();
    }

    private static String normalizar(String valor) {
        return valor == null ? null : valor.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
