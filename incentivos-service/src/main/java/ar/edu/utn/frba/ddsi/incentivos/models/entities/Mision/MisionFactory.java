package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.*;
import org.springframework.stereotype.Component;

@Component
public class MisionFactory {
    private TipoMision tipoMision;

    public Mision crearMision(TipoMision tipo) {
        return switch (tipo) {
            case RACHA -> new Mision("Racha",
                    new ReglaFechaEntrega());
            case HABIL_DONADOR -> new Mision("Habil Donador",
                    new ReglaCantidadBienes());
            case DONACIONES_EXITOSAS -> new Mision("Donaciones Exitosas",
                    new ReglaEstado());
            case COMPLETITUD -> new Mision("Completitud",
                    new ReglaCategoria());
        };
    }
}
