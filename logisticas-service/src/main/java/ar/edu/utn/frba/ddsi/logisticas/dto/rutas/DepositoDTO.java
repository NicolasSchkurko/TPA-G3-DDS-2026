package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Punto fijo de origen de las rutas. Se configura por entorno. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepositoDTO {
    private Double latitud;
    private Double longitud;
    private String etiqueta;
}
