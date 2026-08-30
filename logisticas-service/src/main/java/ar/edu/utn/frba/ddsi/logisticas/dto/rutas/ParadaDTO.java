package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParadaDTO {
    private DireccionDTO direccion;
    private BienesDTO items;

    public ParadaDTO(DireccionDTO direccion, BienesDTO items){
        this.direccion = direccion;
        this.items = items;
    }
}