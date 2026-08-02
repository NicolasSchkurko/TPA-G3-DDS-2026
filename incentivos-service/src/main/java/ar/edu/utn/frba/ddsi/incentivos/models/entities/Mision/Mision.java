package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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

    public void evaluarConstancia() {
        //para evaluar la constancia de una mision:
        //si la ultima donacionExitosa de la persona esta dentro del
        //periodo en la reglaConstancia de la reglaProgreso
        //el progreso se mantiene, sino vuelve a 0
        ReglaConstancia constancia = reglaDeProgreso.getConstancia();

        if (constancia == null || donacionesExitosas.isEmpty()) {
            return;
        }

        ImpactoDonacion ultimaDonacion =
                donacionesExitosas.getLast();

        LocalDateTime limite = ultimaDonacion.getFechaEntrega().plus(
                constancia.getCantidad(),
                constancia.getUnidadTiempo()
        );

        if (LocalDateTime.now().isAfter(limite)) {
            donacionesExitosas.clear();
            this.setProgreso(0);
        }
    }

    public boolean estaCompleta() {
        return reglaDeProgreso.estaCompleta(progreso);
    }

    public void evaluarProgreso(ImpactoDonacion donacion){
        Object valorAtributo = reglaDeProgreso.aplicar(donacion);

        //va a ser 1 o 0 el resultado
        Boolean resultado = reglaDeProgreso.operar(valorAtributo);

        if(resultado){
            donacionesExitosas.add(donacion);
            this.setProgreso(progreso+1);
        }
    }
}
