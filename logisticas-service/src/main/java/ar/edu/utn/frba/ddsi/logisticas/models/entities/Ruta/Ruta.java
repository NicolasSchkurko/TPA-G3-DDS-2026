package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ruta")
@Getter
@Setter
@NoArgsConstructor
public class Ruta {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_ruta", nullable = false, updatable = false)
    private UUID idRuta;

    @ManyToOne
    @JoinColumn(name = "patente_camion", referencedColumnName = "patente", nullable = false)
    private Camion camionAsignado;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoRuta estado;

    @Column(name = "url_seguimiento")
    private String urlSeguimiento; // Enlace al mapa interactivo para tracking en tiempo real

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
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

    public boolean excedeCapacidadDelCamion() {
        return !puedeAgregar(0.0, 0.0);
    }
}