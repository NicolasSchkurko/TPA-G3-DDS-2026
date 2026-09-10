package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
@Getter
@Setter
@Entity
@NoArgsConstructor
public class ValoresDistintos extends Operacion {
    // hacer 6 donaciones de 3 categorias distintas
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JdbcTypeCode(SqlTypes.JSON)
    private List<JsonNode> valoresDistintos;
    private Integer cantValoresDistintos;

    public ValoresDistintos(Integer progresoObjetivo,
                            Integer cantidad) {
        super(progresoObjetivo);
        this.valoresDistintos = new ArrayList<>();
        this.cantValoresDistintos = cantidad;
    }

    @Override
    public Boolean estaCompleta(Integer progresoActual) {
        return progresoActual >= getProgresoObjetivo()
                && valoresDistintos.size() >= cantValoresDistintos;
    }


    @Override
    public Boolean calcularProgreso(Object valorAtributo) {

        JsonNode valor = MAPPER.valueToTree(valorAtributo);

        if (!valoresDistintos.contains(valor)) {
            valoresDistintos.add(valor);
        }

        return true;
    }
}
