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
    private Double latitud;
    private Double longitud;

    public DireccionDTO(UUID idEntidad,
                        String calleUno,
                        String calleDos,
                        Integer altura,
                        Integer piso,
                        String departamento,
                        String ciudad,
                        String provincia,
                        String pais){
        this(idEntidad, calleUno, calleDos, altura, piso, departamento,
                ciudad, provincia, pais, null, null);
    }

    public DireccionDTO(UUID idEntidad,
                        String calleUno,
                        String calleDos,
                        Integer altura,
                        Integer piso,
                        String departamento,
                        String ciudad,
                        String provincia,
                        String pais,
                        Double latitud,
                        Double longitud){
        this.idEntidad = idEntidad;
        this.calleUno = calleUno;
        this.calleDos = calleDos;
        this.altura = altura;
        this.piso = piso;
        this.departamento = departamento;
        this.ciudad = ciudad;
        this.provincia = provincia;
        this.pais = pais;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}