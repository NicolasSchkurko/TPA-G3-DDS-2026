package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Entity;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cuenta las donaciones cuyo atributo coincide con el valor esperado.
 *
 * <p>Es la operación más simple y la que más se usa: "5 donaciones ENTREGADA". El valor
 * esperado se guarda como {@link JsonNode} porque así llega del JSON del admin, sin
 * convertirlo.
 */
@Getter
@Entity
@NoArgsConstructor
public class CantidadCoincidencias extends Operacion {
    // 5 donaciones "ENTREGADAS"
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode valorEsperado;

    public CantidadCoincidencias(
            Integer progresoObjetivo,
            JsonNode valorEsperado
    ) {
        super(progresoObjetivo);
        this.valorEsperado = valorEsperado;
    }

    /**
     * Una donación cuenta si su atributo es igual al esperado, sin distinguir mayúsculas y
     * sin espacio al final.
     *
     * @param donante no se usa: esta operación no necesita acordarse del pasado, solo mira
     *               la donación de ahora.
     */
    @Override
    public boolean calcularProgreso(Object valorAtributo, ProgresoDelDonante donante) {
        if (valorEsperado == null || valorAtributo == null) {
            return false;
        }
        return valorEsperado.asText().equalsIgnoreCase(valorAtributo.toString().trim());
    }

    /**
     * {@code valorEsperado} sí se compara: cambiar de "ENTREGADA" a "RECIBIDA" cambia por
     * completo qué donaciones cuentan, así que lo acumulado deja de servir. El
     * {@code equals} de {@code JsonNode} compara la estructura, no la referencia, así que
     * dos TextNode con el mismo texto dan {@code true}.
     */
    @Override
    public boolean esEquivalenteA(Operacion otra) {
        return super.esEquivalenteA(otra)
               && otra instanceof CantidadCoincidencias otraCoincidencias
               && Objects.equals(valorEsperado, otraCoincidencias.valorEsperado);
    }
}
