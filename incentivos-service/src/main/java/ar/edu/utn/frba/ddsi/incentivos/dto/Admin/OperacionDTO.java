package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OperacionDTO {
    private String tipoOperacion;
    private Integer progresoObjetivo;
    private String valorEsperado;  // solo COINCIDENCIAS
    private Integer cantidad;      // VALORES_DISTINTOS o SUPERA_CANTIDAD

    public OperacionDTO(String tipoOperacion,
                        Integer progresoObjetivo,
                        String valorEsperado,
                        Integer cantidad){
        this.tipoOperacion = tipoOperacion;
        this.progresoObjetivo = progresoObjetivo;
        this.valorEsperado = valorEsperado;
        this.cantidad = cantidad;
    }
}
