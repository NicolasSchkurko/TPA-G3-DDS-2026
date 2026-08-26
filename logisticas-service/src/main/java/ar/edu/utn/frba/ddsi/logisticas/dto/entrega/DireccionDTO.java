package ar.edu.utn.frba.ddsi.logisticas.dto.entrega;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DireccionDTO {
    private UUID idEntidad;
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;

    public DireccionDTO(UUID idEntidad,
                        String calleUno,
                        String calleDos,
                        Integer altura,
                        Integer piso,
                        String departamento,
                        String ciudad,
                        String provincia,
                        String pais){
        this.idEntidad = idEntidad;
        this.calleUno = calleUno;
        this.calleDos = calleDos;
        this.altura = altura;
        this.piso = piso;
        this.departamento = departamento;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.pais = pais;
    }
}
