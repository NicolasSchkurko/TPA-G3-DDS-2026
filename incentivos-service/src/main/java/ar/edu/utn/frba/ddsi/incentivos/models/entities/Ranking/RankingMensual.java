package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
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

    public void agregarPosicion(Ranking ranking) {
        this.posiciones.add(ranking);
        ranking.setRankingMensual(this);
    }

    public void calcularYAgregarPosiciones(List<Object[]> topPerfiles) {
        int puestoActual = 1;
        int indiceGral = 1;
        Long misionesPrevias = -1L;

        for (Object[] fila : topPerfiles) {
            ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil perfil =
                (ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil) fila[0];

            Long totalMisiones = ((Number) fila[1]).longValue();

            if (!totalMisiones.equals(misionesPrevias)) {
                puestoActual = indiceGral;
            }

            Ranking posicion = new Ranking(
                this,
                perfil.getIdUsuario(),
                perfil.getNombreUsuario(),
                puestoActual,
                totalMisiones
            );

            this.agregarPosicion(posicion);

            misionesPrevias = totalMisiones;
            indiceGral++;
        }
    }
}