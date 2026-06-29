package ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.camion.Camion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Ruta {
    private UUID idRuta;
    private Camion camionAsignado;
    private LocalDate fechaProgramada;
    private EstadoRuta estado;
    private List<Parada> paradas;
}