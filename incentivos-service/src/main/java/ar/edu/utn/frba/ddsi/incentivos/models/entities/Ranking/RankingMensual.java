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

    // Spring Data/Hibernate saben convertir YearMonth automáticamente en versiones recientes
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
}