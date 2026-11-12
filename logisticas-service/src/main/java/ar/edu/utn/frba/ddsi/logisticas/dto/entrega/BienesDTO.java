package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BienesDTO {
    private List<UUID> idsDonaciones;
    private List<BienDTO> bienes;

    public BienesDTO(List<UUID> idsDonaciones, List<BienDTO> bienes){
        this.idsDonaciones = idsDonaciones;
        this.bienes = bienes;
    }

    /** Copia los ítems sobre este DTO. Una lista {@code null} se devuelve vacía. */
    public BienesDTO aplicar(List<ItemEntrega> items) {
        if (items == null) {
            this.idsDonaciones = List.of();
            this.bienes = List.of();
            return this;
        }
        this.idsDonaciones = items.stream().map(ItemEntrega::getIdDonacion).toList();
        this.bienes = items.stream().map(item -> new BienDTO().aplicar(item)).toList();
        return this;
    }
}