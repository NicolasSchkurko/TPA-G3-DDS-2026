package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Mision {
    private UUID idMision; // id interno
    private List<ImpactoDonacion> donacionesExitosas;
    private String nombreMision;
    private Insignia insigniaObjetivo;
    private Integer progreso;
    private Regla reglaDeProgreso;

    public Mision(String nombre,
                  String nombreInsignia,
                  ReglaConstancia constancia,
                  Regla regla) {
        this.idMision = UUID.randomUUID();
        this.donacionesExitosas = new ArrayList<>();
        this.nombreMision = nombre;
        this.insigniaObjetivo = new Insignia(nombreInsignia, nombre);
        this.progreso = 0;
        this.reglaDeProgreso = regla;
    }

    //<atributoImpactoDonacion, cantidadTiempo>
    //cantidadTiempo: Period para dias/meses/años, Duration para horas/minutos
    public void evaluarConstancia() {
        ImpactoDonacion ultimaDonacion = donacionesExitosas.getLast();
        T tiempo = Period.between(ultimaDonacion.getFechaEntrega(), );
        // Si pasó 1 mes desde la ultima donación, la lista vuelve a 0
        if (ultimoMes.isBefore(YearMonth.now().minusMonths(1))) {
            mision.getDonacionesExitosas().clear();
        }
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
