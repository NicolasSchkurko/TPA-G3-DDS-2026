package ar.edu.utn.frba.ddsi.incentivos.dto.Perfil;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListaInsigniasDTO {
    private List<InsigniaDTO> insignias;

    public ListaInsigniasDTO(List<InsigniaDTO> insignias){
        this.insignias = insignias;
    }
}
