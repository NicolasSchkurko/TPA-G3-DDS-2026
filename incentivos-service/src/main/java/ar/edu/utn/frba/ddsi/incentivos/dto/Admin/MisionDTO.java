package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionDTO {
    private String nombreMision;
    private String insigniaObjetivo;
    private ReglaDTO regla;

    public MisionDTO(String nomM, String nomI,
                     ConstanciaDTO cia,
                     String atributo,
                     OperacionDTO op){
        this.nombreMision = nomM;
        this.insigniaObjetivo = nomI;
        this.regla = new ReglaDTO(cia, atributo, op);
    }
}