package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReglaDTO {
    private ConstanciaDTO constancia;
    private String atributo;
    private OperacionDTO operacion;

    public ReglaDTO(ConstanciaDTO constancia,
                    String atributo,
                    OperacionDTO operacion){
        this.constancia = constancia;
        this.atributo = atributo;
        this.operacion = operacion;
    }
}
