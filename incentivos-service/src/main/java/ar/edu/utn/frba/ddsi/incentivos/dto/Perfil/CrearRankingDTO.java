package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.YearMonth;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearRankingDTO {
    private YearMonth periodo;
}

