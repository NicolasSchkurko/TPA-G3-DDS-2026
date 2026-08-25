package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MisionDTO {
    private String nombreMision;
    private String descripcion;
    private String insigniaObjetivo;
    private ReglaDTO regla;

    public MisionDTO(String nomM, String descripcion, String nomI,
                     ConstanciaDTO cia,
                     String atributo,
                     OperacionDTO op){
        this.nombreMision = nomM;
        this.descripcion = descripcion;
        this.insigniaObjetivo = nomI;
        this.regla = new ReglaDTO(cia, atributo, op);
    }
}