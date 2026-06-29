package ar.edu.utn.frba.ddsi.logisticas.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntregaDTO {
    //donacion a entregar
    private UUID idDonacion;
    private UUID idPersonaDonante; //?
    private String descripcion;
    private String estado;
    private String subCategoria;
    private LocalDate fechaEntrega;
    private List<BienDTO> bienes;
}
