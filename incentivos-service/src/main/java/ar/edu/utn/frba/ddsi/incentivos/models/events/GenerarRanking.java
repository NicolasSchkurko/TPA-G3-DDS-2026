package ar.edu.utn.frba.ddsi.incentivos.models.events;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.time.YearMonth;
import java.util.List;

public record GenerarRanking(YearMonth periodo, List<Perfil> perfiles) {
}
