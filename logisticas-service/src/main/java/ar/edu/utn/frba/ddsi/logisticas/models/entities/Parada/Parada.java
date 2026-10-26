package ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "parada")
@Getter
@Setter
@NoArgsConstructor
public class Parada {
    @Id
    @Field(name = "id_parada")
    private UUID idParada;

    @Version
    private Long version;

    private Ruta ruta;

    private Direccion direccion;

    private Entidad entidadDestino;

    private List<ItemEntrega> items = new ArrayList<>();

    // Se crea siempre a partir de un primer itm y su dirección
    public Parada(ItemEntrega primerItem) {
        Entidad entidad = primerItem.getEntidadDestino();
        this.entidadDestino = entidad;
        this.direccion = entidad.getDireccionDestino();
        this.agregarItem(primerItem);
    }

    public void agregarItem(ItemEntrega item) {
        items.add(item);
    }
}