package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;


import java.util.List;

public class Racha extends Mision {
    Integer mesesObjetivo;

    public Racha(Insignia insignia, String descripcion, Integer mesesObjetivo) {
        super(insignia, descripcion);
        this.mesesObjetivo = mesesObjetivo;
    }

//    @Override
//    public Integer getProgresoObjetivo() {
//        return this.mesesObjetivo;
//    }
//
//    @Override
//    public Integer calcularProgresoActual(Perfil perfil) {
//        Set<YearMonth> mesesConDonacion = perfil.getDonaciones().stream()
//                .filter(d -> "ENTREGADA".equalsIgnoreCase(d.getEstado()))
//                .filter(d -> d.getFechaEntrega() != null)
//                .map(d -> YearMonth.from(d.getFechaEntrega()))
//                .collect(Collectors.toSet());
//
//        YearMonth mesAExaminar = YearMonth.now();
//        int rachaActual = 0;
//
//        if (!mesesConDonacion.contains(mesAExaminar)) {
//            mesAExaminar = mesAExaminar.minusMonths(1);
//        }
//
//        while (mesesConDonacion.contains(mesAExaminar)) {
//            rachaActual++;
//            mesAExaminar = mesAExaminar.minusMonths(1);
//        }
//        return rachaActual;
//    }
//
//    @Override
//    public Boolean completarMision(Perfil perfil) {
//        return this.calcularProgresoActual(perfil) >= this.getProgresoObjetivo();
//    }
}
