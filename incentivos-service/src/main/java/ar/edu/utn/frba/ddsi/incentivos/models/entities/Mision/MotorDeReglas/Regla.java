package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Regla {
    private String nombreRegla;

    public Regla(String nombre) {
        this.nombreRegla = nombre;
    }

    public abstract void aplicar(ImpactoDonacion donacion, Mision mision);

    public abstract void evaluarProgreso(Mision mision);
}
