package ar.edu.utn.frba.ddsi.logisticas.dto;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DestinoEntregaDTO {
    private UUID idEntidadBeneficiaria;
    //direccion de la entidad beneficiaria
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;
    //la donacion a entregar
    private List<EntregaDTO> entregas; //1 ENTIDAD PUEDE RECIBIR +1 DONACION?
}
