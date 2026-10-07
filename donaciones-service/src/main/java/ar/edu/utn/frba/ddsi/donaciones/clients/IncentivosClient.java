package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Le avisa a incentivos-service lo que necesita saber.
 *
 * <p>Las rutas reales de incentivos son {@code POST /api/perfiles} y
 * {@code PATCH /api/perfiles/donacion/{idUsuario}}; difieren en método y en que una lleva
 * path variable. La propiedad apunta a la base del servicio y cada método compone su propio
 * sufijo (un cambio de puerto no requiere tocar rutas). Los errores se loguean con nivel y
 * URL antes de relanzar: un fallo de integración no debe confundirse con un noop.
 */
@Service
public class IncentivosClient {

    private static final Logger log = LoggerFactory.getLogger(IncentivosClient.class);

    /** Sufijo del alta de perfil. */
    private static final String RUTA_PERFILES = "/api/perfiles";

    /** Sufijo del avance por donación, con el id del usuario como path variable. */
    private static final String RUTA_DONACION = "/api/perfiles/donacion";

    private final RestTemplate restTemplate;

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    public IncentivosClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /** Da de alta el perfil del donante en incentivos: {@code POST /api/perfiles}. */
    public void peticionCrearPerfil(IDDTO dto) {
        try {
            restTemplate.postForEntity(url(RUTA_PERFILES), dto, Void.class);
            log.debug("Perfil creado en incentivos para {}", dto.getIdUsuario());
        } catch (Exception e) {
            log.error("No se pudo crear el perfil {} en {}: {}",
                    dto.getIdUsuario(), url(RUTA_PERFILES), e.getMessage());
            throw e;
        }
    }

    /** Avanza el perfil del donante: {@code PATCH /api/perfiles/donacion/{idUsuario}}. */
    public void notificarDonacionAsignada(UUID idUsuario, IncentivosDonacionDTO dto) {
        String url = url(RUTA_DONACION + "/" + idUsuario);

        try {
            restTemplate.exchange(url, HttpMethod.PATCH, new HttpEntity<>(dto), Void.class);
            log.debug("Donación notificada a incentivos para {}", idUsuario);
        } catch (Exception e) {
            log.error("No se pudo notificar la donación de {} en {}: {}",
                    idUsuario, url, e.getMessage());
            throw e;
        }
    }

    /**
     * Quita la barra final para poder componer rutas sin duplicar separadores.
     *
     * <p>La propiedad puede venir como {@code http://host:8082} o
     * {@code http://host:8082/}, y sin normalizar la primera ruta salía como
     * {@code http://host:8082//api/perfiles}, que no matchea ningún mapping.
     */
    private String url(String ruta) {
        return incentivosUrl.replaceAll("/+$", "") + ruta;
    }
}
