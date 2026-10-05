package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Un valor del atributo de una donación que el donante ya vio mientras hacía su
 * misión actual.
 *
 * <p>Es una fila por (donante, misión, valor). El {@link ProgresoMision} al que apunta
 * es lo que la individualiza: dos donantes haciendo la misma misión tienen conjuntos de
 * filas completamente distintos, y por eso uno puede haber visto "Ropa" y "Alimentos"
 * mientras el otro solo vio "Ropa", sin que interfieran.
 *
 * <p>La restricción única sobre (progreso_mision_id, valor) hace que el mismo valor no
 * se pueda contar dos veces para el mismo donante.
 */
@Getter
@Setter
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_valor_observado_progreso_valor",
        columnNames = {"progreso_mision_id", "valor"}))
@NoArgsConstructor
public class ValorObservado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "progreso_mision_id", nullable = false)
    private ProgresoMision progresoMision;

    @Column(name = "valor", nullable = false, length = 512)
    private String valor;

    public ValorObservado(ProgresoMision progresoMision, String valor) {
        this.progresoMision = progresoMision;
        this.valor = valor;
    }
}
