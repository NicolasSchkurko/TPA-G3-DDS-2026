package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.ReglaCantidadBienes;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Mision {
    private List<ImpactoDonacion> donacionesExitosas;
    private String nombreMision;
    private Boolean constante;
    private Insignia insigniaObjetivo;
    private Integer progresoObjetivo;
    private Regla reglaDeProgreso; //patron factory method

    public Mision(String nombre,
                  Regla regla) {
        this.donacionesExitosas = new ArrayList<>();
        this.nombreMision = nombre;
        this.insigniaObjetivo = null; //se inicializa en el repositorio de misiones
        this.progresoObjetivo = null;
        this.reglaDeProgreso = regla;
    }

    public void evaluarConstancia() {
        reglaDeProgreso.evaluarProgreso(this);
    }

    public Integer getProgresoActual() {
        return reglaDeProgreso.conseguirProgreso(donacionesExitosas);
    }

    public boolean estaCompleta() {
        return this.getProgresoActual() >= this.progresoObjetivo;
    }

    public void evaluarDonacion(ImpactoDonacion donacion){
        reglaDeProgreso.aplicar(donacion, this);
    }
}
