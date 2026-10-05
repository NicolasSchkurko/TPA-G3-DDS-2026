package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * El ranking de un mes, ya publicado.
 *
 * <p>Es un snapshot y no una consulta: una vez generado no se recalcula. Si mañana cambia
 * el criterio de las misiones, el ranking de marzo sigue mostrando lo que pasó en marzo.
 *
 * <p>No tiene setters. El período se fija al construir y las posiciones se agregan con
 * {@link #agregarPosicion}; con setters abiertos se podía cambiar el {@code periodo} de un
 * ranking ya publicado, y como {@code periodo} es {@code unique} eso además reventaba por
 * FK o por restricción.
 */
@Getter
@Entity
@NoArgsConstructor
public class RankingMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idRanking;

    @Column(nullable = false, unique = true)
    private YearMonth periodo;

    @OneToMany(mappedBy = "rankingMensual", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("puesto ASC")
    private List<Ranking> posiciones = new ArrayList<>();

    public RankingMensual(YearMonth periodo) {
        this.periodo = periodo;
    }

    /**
     * Agrega una fila al podio y la enlaza con este ranking.
     *
     * <p>Es el único camino para sumar posiciones: {@link #calcularYAgregarPosiciones} lo
     * usa para todas, así que el {@code mappedBy} de la lista nunca queda apuntando al
     * revés.
     */
    public void agregarPosicion(Ranking ranking) {
        this.posiciones.add(ranking);
    }

    /**
     * Traduce el resultado de la consulta de ranking en posiciones.
     *
     * <p>La consulta viene ordenada por puntaje descendente, así que el puesto se deduce
     * del lugar en que aparece cada fila. Los empates comparten puesto: si dos filas
     * seguidas tienen el mismo total, la segunda no avanza el número, como en
     * cualquier tabla de posiciones.
     *
     * @param topPerfiles filas de {@code [Perfil, total]}, ya ordenadas por total
     *                    descendente.
     */
    public void calcularYAgregarPosiciones(List<Object[]> topPerfiles) {
        int puestoActual = 1;
        int indiceGeneral = 1;
        Long misionesPrevias = -1L;

        for (Object[] fila : topPerfiles) {
            Perfil perfil = (Perfil) fila[0];
            Long totalMisiones = ((Number) fila[1]).longValue();

            if (!totalMisiones.equals(misionesPrevias)) {
                puestoActual = indiceGeneral;
            }

            agregarPosicion(new Ranking(
                    this,
                    perfil.getIdUsuario(),
                    perfil.getNombreUsuario(),
                    puestoActual,
                    totalMisiones
            ));

            misionesPrevias = totalMisiones;
            indiceGeneral++;
        }
    }
}
