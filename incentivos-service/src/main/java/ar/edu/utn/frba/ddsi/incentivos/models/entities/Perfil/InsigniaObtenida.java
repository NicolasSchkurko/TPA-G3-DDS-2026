package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * La insignia que un donante ya obtuvo, y cuándo. No se edita: la fila se crea al obtenerla.
 * Los tres índices cubren el detalle por insignia, la paginación por perfil y el ranking
 * mensual.
 */
@Getter
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_insignia_obtenida_perfil_insignia",
                columnNames = {"perfil_id", "insignia_id"}),
        indexes = {
                @Index(name = "idx_insignia_obtenida_insignia", columnList = "insignia_id"),
                @Index(name = "idx_insignia_obtenida_perfil_fecha",
                        columnList = "perfil_id, fecha_obtencion"),
                @Index(name = "idx_insignia_obtenida_fecha", columnList = "fecha_obtencion")
        })
@NoArgsConstructor
public class InsigniaObtenida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;

    /**
     * La insignia conseguida, {@code LAZY} para no disparar una consulta por fila. Quien
     * necesita el nombre la trae con {@code @EntityGraph}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insignia_id")
    private Insignia insignia;

    private LocalDateTime fechaObtencion;

    public InsigniaObtenida(Perfil perfil, Insignia insignia) {
        this.perfil = perfil;
        this.insignia = insignia;
        this.fechaObtencion = LocalDateTime.now();
    }

    /**
     * Dos Obtenidas son la misma si son del mismo perfil y de la misma insignia. Es lo que
     * permite deduplicar en el {@code Set} de {@code Perfil}.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof InsigniaObtenida otra)) {
            return false;
        }
        return esMismoPerfil(perfil, otra.perfil)
                && Objects.equals(insignia, otra.insignia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perfil == null ? null : perfil.getIdUsuario(), insignia);
    }

    /** Compara por id para tolerar que Hibernate entregue el back-reference como proxy. */
    private static boolean esMismoPerfil(Perfil una, Perfil otra) {
        if (una == null || otra == null) {
            return false;
        }
        if (una.getIdUsuario() == null || otra.getIdUsuario() == null) {
            return una == otra;
        }
        return una.getIdUsuario().equals(otra.getIdUsuario());
    }
}
