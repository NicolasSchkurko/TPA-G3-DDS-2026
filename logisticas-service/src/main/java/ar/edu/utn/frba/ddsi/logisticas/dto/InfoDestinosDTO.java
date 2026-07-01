package ar.edu.utn.frba.ddsi.logisticas.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InfoDestinosDTO {
    List<DestinoEntregaDTO> rutas;
}
