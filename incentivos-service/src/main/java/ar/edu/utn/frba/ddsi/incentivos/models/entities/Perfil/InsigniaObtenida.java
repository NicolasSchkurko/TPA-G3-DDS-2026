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
 * La insignia que un donante ya obtuvo, y cuándo.
 *
 * <p>No tiene setters: la fila se crea cuando el donante obtiene la insignia y no se
 * edita. La fecha de obtención la pone el constructor.
 *
 * <p><b>Los tres índices no son adorno</b> (punto 22). Esta es la tabla que más crece: una
 * fila por cada insignia que obtiene cada donante. Las consultas que la leen filtran por
 * columnas distintas y sin índice cada una es un recorrido completo de la tabla:
 *
 * <ul>
 *   <li>{@code (insignia_id)} para el detalle de una insignia en particular.</li>
 *   <li>{@code (perfil_id, fecha_obtencion)} para la paginación de las insignias de un
 *       donante, que ordena por fecha de obtención. El orden de las columnas importa: al
 *       revés, la base puede usar el índice para filtrar por perfil pero igual tiene que
 *       ordenar por fecha, que es la parte cara.</li>
 *   <li>{@code (fecha_obtencion)} a secas, para el ranking mensual. Ese filtro no sabe
 *       todavía de qué perfil se trata: agrupa por donante sobre todo el mes, así que
 *       ninguno de los otros dos índices le sirve.</li>
 * </ul>
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
     * La insignia conseguida.
     *
     * <p><b>LAZY y no EAGER</b> (punto 22). Con {@code EAGER}, Hibernate la trae siempre, y
     * como las consultas que devuelven {@code InsigniaObtenida} suelen devolver una lista, eso
     * es una consulta extra por cada fila: la paginación de 20 insignias de un donante eran
     * 21 consultas. Ahora es una sola, y las dos lecturas que necesitan el nombre de la
     * insignia usan {@code paginaInsigniasPorIdUsuario}, que trae la relación en la misma
     * consulta con un {@code @EntityGraph}.
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
     * Dos Obtenidas son la misma si son del mismo perfil y de la misma insignia.
     *
     * <p>Hace falta para que {@code Perfil.insigniasObtenidas} sea un {@code Set} y sirva
     * de deduplicación: sin {@code equals}, un {@code Set} de entidades compara por
     * identidad y nunca reconoce dos filas que son la misma insignia (punto 28).
     *
     * <p>La clave es (perfil, insignia) y no el id, justamente para que dos objetos
     * distintos que representan lo mismo se reconozcan.
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
