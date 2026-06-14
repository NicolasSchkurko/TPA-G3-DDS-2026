package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Mision {
    private List<ImpactoDonacion> donacionesExitosas = new ArrayList<>();
    private String nombreMision;
    private Insignia insigniaObjetivo;
    private Integer progresoActual;
    private Integer progresoObjetivo;

    public Mision(String nombre,
                  Insignia insignia,
                  Integer objetivo) {
        this.donacionesExitosas = new ArrayList<>();
        this.nombreMision = nombre;
        this.insigniaObjetivo = insignia;
        this.progresoActual = 0;
        this.progresoObjetivo = objetivo;
    }

    public Integer getProgresoActual() {
        return this.donacionesExitosas.size();
    }

    public boolean estaCompleta() {
        return this.getProgresoActual() >= this.progresoObjetivo;
    }

    public abstract void evaluarDonacion(ImpactoDonacion donacion);
}
