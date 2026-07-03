package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
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
    private String urlSeguimiento; // Enlace al mapa interactivo para tracking en tiempo real
    private List<Parada> paradas = new ArrayList<>();

    // Agrupa por entidad: si ya hay una Parada para esa entidad en esta ruta, se agrega al listado
    public void agregarEntrega(ItemEntrega item) {
        paradas.stream()
               .filter(p -> p.getEntidadDestino().equals(item.getEntidadDestino()))
               .findFirst()
               .ifPresentOrElse(
                   parada -> parada.agregarItem(item),
                   () -> paradas.add(new Parada(item))
               );
    }

    // Todos los items que lleva el camión en esta ruta, sin importar en qué parada van.
    public List<ItemEntrega> obtenerTodosLosItems() {
        return paradas.stream()
                      .flatMap(p -> p.getItems().stream())
                      .collect(Collectors.toList());
    }

    public Double pesoTotalCargadoKg() {
        return obtenerTodosLosItems().stream().mapToDouble(ItemEntrega::getPesoEstimadoKg).sum();
    }

    public Double volumenTotalCargadoM3() {
        return obtenerTodosLosItems().stream().mapToDouble(ItemEntrega::getVolumenEstimadoM3).sum();
    }

    public boolean puedeAgregar(Double pesoAdicionalKg, Double volumenAdicionalM3) {
        return camionAsignado.puedeCargar(pesoTotalCargadoKg() + pesoAdicionalKg, volumenTotalCargadoM3() + volumenAdicionalM3);
    }
}