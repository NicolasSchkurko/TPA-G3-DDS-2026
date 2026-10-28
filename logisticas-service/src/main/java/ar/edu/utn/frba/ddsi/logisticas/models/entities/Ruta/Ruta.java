package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "ruta")
@Getter
@Setter
@NoArgsConstructor
public class Ruta {
    @Id
    @Field(name = "id_ruta")
    private UUID idRuta;

    @Version
    private Long version;

    private Camion camionAsignado;

    @Field(name = "fecha_programada")
    private LocalDate fechaProgramada;

    @Field(name = "estado")
    private EstadoRuta estado;

    @Field(name = "url_seguimiento")
    private String urlSeguimiento; // Enlace al mapa interactivo para tracking en tiempo real

    private List<Parada> paradas;

    public Ruta(Camion camion){
        this.idRuta = UUID.randomUUID();
        this.camionAsignado = camion;
        this.fechaProgramada = LocalDate.now().plusDays(1);
        this.paradas = new ArrayList<>();
    }

    public void agregarEntrega(ItemEntrega item) {
        if (item == null || item.getEntidadDestino() == null) {
            throw new IllegalArgumentException("El ítem de entrega y su entidad destino no pueden ser nulos.");
        }

        paradas.stream()
                .filter(p -> Objects.equals(p.getEntidadDestino(), item.getEntidadDestino()))
                .findFirst()
                .ifPresentOrElse(
                        parada -> parada.agregarItem(item),
                        () -> {
                            Parada parada = new Parada(item);
                            parada.setRuta(this);
                            paradas.add(parada);
                        }
                );
    }

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
