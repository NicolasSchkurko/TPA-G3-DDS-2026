package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Ruta {
    private UUID idRuta;
    private Camion camionAsignado;
    private LocalDate fechaProgramada;
    private EstadoRuta estado;
    private List<Parada> paradas = new ArrayList<>();

    // Todos los items que lleva el camión en esta ruta.
    public List<ItemEntrega> obtenerTodosLosItems() {
        return paradas.stream()
                      .flatMap(p -> p.getItems().stream())
                      .collect(Collectors.toList());
    }

    public Double pesoTotalCargadoKg() {
        return paradas.stream().mapToDouble(Parada::pesoTotalKg).sum();
    }

    public Double volumenTotalCargadoM3() {
        return paradas.stream().mapToDouble(Parada::volumenTotalM3).sum();
    }

    // Chequeo de capacidad contra el camión asignado.
    public boolean excedeCapacidadDelCamion() {
        return !camionAsignado.puedeCargar(pesoTotalCargadoKg(), volumenTotalCargadoM3());
    }

    // Dispara el inicio de ruta: la ruta pasa a EN_CURSO y todos sus items a EN_TRASLADO.
    public void iniciar() {
        if (estado != EstadoRuta.PROGRAMADA) {
            throw new IllegalStateException("Solo se puede iniciar una ruta PROGRAMADA, estado actual: " + estado);
        }
        this.estado = EstadoRuta.EN_CURSO;
        obtenerTodosLosItems().forEach(ItemEntrega::iniciarTraslado);
    }

    // La ruta se da por finalizada cuando ya no quedan items EN_TRASLADO
    public boolean estaFinalizada() {
        return obtenerTodosLosItems().stream()
                                     .noneMatch(item -> item.getEstado() == EstadoEntrega.EN_TRASLADO);
    }
}