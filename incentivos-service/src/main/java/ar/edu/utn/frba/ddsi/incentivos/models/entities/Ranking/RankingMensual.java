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
 * El ranking de un mes ya publicado. Es un snapshot: una vez generado no se recalcula. No
 * tiene setters; el período se fija al construir.
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

    /** Agrega una fila al podio y la enlaza con este ranking. */
    public void agregarPosicion(Ranking ranking) {
        this.posiciones.add(ranking);
    }

    /**
     * Traduce el resultado de la consulta en posiciones. Los empates comparten puesto.
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
