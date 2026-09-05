package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Getter
//patron strategy
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@NoArgsConstructor
public abstract class Operacion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idOperacion;

    private Integer progresoObjetivo;

    protected Operacion(Integer progresoObjetivo) {
        this.idOperacion = UUID.randomUUID();
        this.progresoObjetivo = progresoObjetivo;
    }

    public Boolean estaCompleta(Integer progresoActual) {
        return progresoActual >= progresoObjetivo;
    }

    public abstract Boolean calcularProgreso(
            Object valorAtributo
    );
}
