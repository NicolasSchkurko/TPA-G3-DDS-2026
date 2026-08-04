package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConstanciaDTO {
    private Integer cantidad;
    private String unidadTiempo;

    public ConstanciaDTO(Integer cant,
                         String unidad){
        this.cantidad = cant;
        this.unidadTiempo = unidad;
    }
}
