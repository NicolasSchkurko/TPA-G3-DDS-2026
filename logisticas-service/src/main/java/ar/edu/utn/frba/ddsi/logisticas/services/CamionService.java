package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.ItemEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta.Ruta;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ar.edu.utn.frba.ddsi.logisticas.models.entities.ruta.EstadoRuta.*;

public class CamionService {
    public void crearRuta(dto) {
        //TODO elegir camion para la ruta
        new Ruta(UUID.randomUUID(), camionAsignado, LocalDate.now(), PROGRAMADA, new ArrayList<>());
    }

    public void actualizarRuta(ItemEntregaDTO) {
        //TODO actualizar ruta para agregar una parada por item de entrega
    }
}