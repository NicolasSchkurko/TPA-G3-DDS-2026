package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Index;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
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
@Table(indexes = {
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
        return Objects.equals(perfil, otra.perfil)
                && Objects.equals(insignia, otra.insignia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perfil, insignia);
    }
}
