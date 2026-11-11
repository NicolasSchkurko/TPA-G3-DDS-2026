package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioDeDirecciones extends JpaRepository<Direccion, UUID> {

    /**
     * Direcciones que el cron de geocoding todavía puede reintentar.
     *
     * <p>Se excluyen:
     * <ul>
     *   <li>las que ya tienen ambas coordenadas;</li>
     *   <li>las anonimizadas por soft delete ({@code calleUno == null});</li>
     *   <li>las que ya agotaron los intentos;</li>
     *   <li>las intentadas hace menos de {@code limite} (backoff entre corridas).</li>
     * </ul>
     *
     * <p>Se pide siempre la primera página: cada corrida actualiza
     * {@code ultimoIntentoGeocoding}, así que las filas salen del resultado en la próxima
     * iteración. No se usa offset para no saltear filas mientras se modifican.
     */
    @Query("""
            select d from Direccion d
            where (d.latitud is null or d.longitud is null)
              and d.calleUno is not null
              and d.calleUno <> ''
              and d.intentosGeocoding < :intentosMaximos
              and (d.ultimoIntentoGeocoding is null or d.ultimoIntentoGeocoding < :limite)
            order by d.id
            """)
    List<Direccion> buscarPendientesGeocoding(
            @Param("limite") LocalDateTime limite,
            @Param("intentosMaximos") int intentosMaximos,
            Pageable pageable);

    /**
     * Relee las direcciones con lock de escritura antes de aplicar resultados de Georef.
     *
     * <p>Evita pisar coordenadas que otra instancia haya guardado mientras esta esperaba la
     * respuesta HTTP.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Direccion d where d.id in :ids")
    List<Direccion> buscarParaActualizar(@Param("ids") Collection<UUID> ids);
}
