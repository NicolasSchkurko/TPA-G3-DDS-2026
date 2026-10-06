package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.PublicacionPendienteN8n;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPublicacionesPendientes;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operaciones transaccionales para encolar y reclamar publicaciones de n8n. */
@Service
public class PublicacionesN8nService {

    private static final long LEASE_SEGUNDOS = 60;
    private static final long RETRASO_BASE_SEGUNDOS = 30;
    private static final long RETRASO_MAXIMO_SEGUNDOS = 3600;

    private final RepositorioPublicacionesPendientes repositorio;

    public PublicacionesN8nService(RepositorioPublicacionesPendientes repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Persiste el evento en la transaccion activa que tambien guarda el perfil.
     *
     * @param publicacion contenido a publicar
     */
    @Transactional
    public void encolar(PerfilPublicacionDTO publicacion) {
        repositorio.save(new PublicacionPendienteN8n(publicacion, LocalDateTime.now()));
    }

    /**
     * Reclama una publicacion disponible y confirma el lease antes de llamar a n8n.
     *
     * @param ahora instante usado para buscar y fijar el lease
     * @return publicacion reclamada, si hay una disponible
     */
    @Transactional
    public Optional<PublicacionReclamada> reclamarSiguiente(LocalDateTime ahora) {
        return repositorio.buscarSiguienteParaEnviar(ahora)
                .map(publicacion -> {
                    publicacion.reclamarHasta(ahora.plusSeconds(LEASE_SEGUNDOS));
                    return new PublicacionReclamada(publicacion.getId(), publicacion.comoDto(),
                            publicacion.getIntentos());
                });
    }

    /**
     * Elimina una publicacion que n8n confirmo.
     *
     * @param id identificador de la fila enviada
     */
    @Transactional
    public void confirmar(UUID id) {
        repositorio.deleteById(id);
    }

    /**
     * Libera el lease y agenda el siguiente intento con espera exponencial acotada.
     *
     * @param id identificador de la fila fallida
     * @param ahora instante de referencia
     * @param error detalle retornado por el cliente HTTP
     */
    @Transactional
    public void reintentar(UUID id, LocalDateTime ahora, String error) {
        PublicacionPendienteN8n publicacion = repositorio.findById(id)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la publicacion pendiente " + id));
        int exponente = Math.min(publicacion.getIntentos() - 1, 7);
        long espera = Math.min(RETRASO_BASE_SEGUNDOS * (1L << exponente),
                RETRASO_MAXIMO_SEGUNDOS);
        publicacion.reintentarEn(ahora.plusSeconds(espera), error);
    }

    /** Una fila reclamada y lista para enviar fuera de la transaccion de base de datos. */
    public record PublicacionReclamada(UUID id,
                                       PerfilPublicacionDTO contenido,
                                       int intentos) {
    }
}
