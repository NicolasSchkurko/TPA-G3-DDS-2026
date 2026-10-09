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
 * Cuenta las donaciones cuyo atributo coincide con el valor esperado. Se guarda como
 * {@link JsonNode} porque así llega del JSON del admin.
 */
@Getter
@Entity
@NoArgsConstructor
public class CantidadCoincidencias extends Operacion {
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
     * Una donación cuenta si su atributo iguala al esperado, sin mayúsculas ni espacios.
     *
     * @param donante no se usa: solo mira la donación actual.
     */
    @Override
    public boolean calcularProgreso(Object valorAtributo, ProgresoDelDonante donante) {
        if (valorEsperado == null || valorAtributo == null) {
            return false;
        }
        return valorEsperado.asText().equalsIgnoreCase(valorAtributo.toString().trim());
    }

    /**
     * {@code valorEsperado} sí se compara: cambiarlo cambia qué donaciones cuentan. El
     * {@code equals} de {@code JsonNode} compara la estructura.
     */
    @Override
    public boolean esEquivalenteA(Operacion otra) {
        return super.esEquivalenteA(otra)
               && otra instanceof CantidadCoincidencias otraCoincidencias
               && Objects.equals(valorEsperado, otraCoincidencias.valorEsperado);
    }
}
