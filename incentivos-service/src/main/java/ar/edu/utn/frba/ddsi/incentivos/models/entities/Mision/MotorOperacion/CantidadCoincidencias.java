package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion;

import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class CantidadCoincidencias extends Operacion{
    //5 donaciones "ENTREGADAS"
    @JdbcTypeCode(SqlTypes.JSON)
    private Object valorEsperado;

    public CantidadCoincidencias(Integer progresoObjetivo,
                                 Object valorEsperado) {
        super(progresoObjetivo);
        this.valorEsperado = valorEsperado;
    }

    @Override
    public Boolean calcularProgreso(
            Object valorAtributo
    ){
        return valorAtributo.equals(valorEsperado);
    }
}
