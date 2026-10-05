package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "El ranking requiere un periodo (por ejemplo 2026-10)")
    private YearMonth periodo;
}

