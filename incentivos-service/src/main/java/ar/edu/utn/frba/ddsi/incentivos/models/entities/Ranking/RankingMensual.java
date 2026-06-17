package ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class RankingMensual {
    private YearMonth periodo;
    private List<PosicionRanking> posiciones;

    public RankingMensual(YearMonth periodo) {
        this.periodo = periodo;
        this.posiciones = new ArrayList<>();
    }

    public void agregarPosicion(Perfil perfil, int posicion) {
        posiciones.add(new PosicionRanking(posicion,
                perfil.getIdPerfil(),
                perfil.getIdUsuario(),
                perfil.getNombreUsuario(),
                perfil.getMisionesCumplidasEnPeriodo()));
    }
}
