package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ParadaDTO {
    private DireccionDTO direccion;
    private BienesDTO items;

    public ParadaDTO(DireccionDTO direccion, BienesDTO items){
        this.direccion = direccion;
        this.items = items;
    }

    /** Copia los datos de la parada sobre este DTO. La parada no puede ser {@code null}. */
    public ParadaDTO aplicar(Parada parada) {
        this.direccion = new DireccionDTO().aplicar(parada.getEntidadDestino());
        this.items = new BienesDTO().aplicar(parada.getItems());
        return this;
    }
}