package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.Regla;
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
    private Regla reglaDeProgreso; //estrategia: motor de condiciones/reglas

    public Mision(String nombre,
                  Regla regla) {
        this.donacionesExitosas = new ArrayList<>();
        this.nombreMision = nombre;
        this.insigniaObjetivo = null; //se inicializa en el repositorio de misiones
        this.progresoObjetivo = null;
        this.reglaDeProgreso = regla;
    }

    public void evaluarConstancia() {
        Mision mision = this;
        reglaDeProgreso.evaluarProgreso(mision);
        this.donacionesExitosas = mision.getDonacionesExitosas();
    }

    public Integer getProgresoActual() {
        if(reglaDeProgreso.getNombreRegla().equals("reglaCantidadBienes")) {
            return this.donacionesExitosas.stream().mapToInt(ImpactoDonacion::getCantidadBienes).sum();
        }
        return this.donacionesExitosas.size();
    }

    public boolean estaCompleta() {
        return this.getProgresoActual() >= this.progresoObjetivo;
    }

    public void evaluarDonacion(ImpactoDonacion donacion){
        Mision mision = this;
        reglaDeProgreso.aplicar(donacion, mision);
        this.donacionesExitosas = mision.getDonacionesExitosas();
    }
}
