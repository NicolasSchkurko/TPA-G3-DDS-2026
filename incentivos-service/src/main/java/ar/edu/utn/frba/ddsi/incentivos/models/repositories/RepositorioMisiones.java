package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.*;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaCantidadBienes;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaEstado;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaFechaEntrega;

import java.util.ArrayList;
import java.util.List;

public class RepositorioMisiones {
    private final List<Mision> misiones;

    private RepositorioMisiones() {
        this.misiones = new ArrayList<>();
        inicializarMisionesBase();
    }

    private void inicializarMisionesBase() {
        Mision mision = new Mision("racha",
                new ReglaFechaEntrega());
        Insignia insignia = new Insignia(mision.getNombreMision(),
                "realizar donaciones durante" + mision.getProgresoObjetivo() + " meses consecutivos");
        mision.setInsigniaObjetivo(insignia);
        this.misiones.add(mision);

        mision = new Mision("completitud",
                new ReglaCategoria());
        insignia = new Insignia(mision.getNombreMision(),
                "hacer " + mision.getProgresoObjetivo() + " donaciones de categorias distintas");
        mision.setInsigniaObjetivo(insignia);
        this.misiones.add(mision);

        mision = new Mision("habil donador",
                new ReglaCantidadBienes());
        insignia = new Insignia(mision.getNombreMision(),
                "donar " + mision.getProgresoObjetivo() + " bienes");
        mision.setInsigniaObjetivo(insignia);
        this.misiones.add(mision);

        mision = new Mision("donacion exitosa",
                new ReglaEstado());
        insignia = new Insignia(mision.getNombreMision(),
                "realizar " + mision.getProgresoObjetivo() + " donaciones exitosas");
        mision.setInsigniaObjetivo(insignia);
        this.misiones.add(mision);
    }

    public Mision buscarPorNombre(String nombre) {
        if (nombre == null) return null;
        return this.misiones.stream()
                .filter(m -> m.getNombreMision().equals(nombre))
                .findFirst()
                .orElse(null);
    }

    public List<Mision> obtenerTodas() {
        return new ArrayList<>(this.misiones);
    }
}
