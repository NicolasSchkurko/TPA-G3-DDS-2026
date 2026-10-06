package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrearRankingDTO {

    @NotNull(message = "El ranking requiere un periodo (por ejemplo 2026-10)")
    private YearMonth periodo;
}

