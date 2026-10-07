package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Qué tiene que cumplir el donante para completar una misión: sobre qué atributo se lo mide,
 * con qué operación y con qué condición de tiempo. No tiene setters.
 */
@Getter
@Entity
@NoArgsConstructor
public class Regla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idRegla;

    @OneToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "constancia_id")
    private ReglaConstancia constancia; // puede ser null

    // Sin orphanRemoval: la regla no se modifica en el lugar, y el cascade = ALL ya borra su
    // constancia y operación al reemplazarla.

    @Enumerated(EnumType.STRING)
    private AtributoImpacto atributo; // atributo de ImpactoDonacion

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion; // define relacion entre atributo y lista donaciones

    public Regla(
            ReglaConstancia constancia,
            AtributoImpacto atributo,
            Operacion operacion
    ) {
        this.constancia = constancia;
        this.atributo = atributo;
        this.operacion = operacion;
    }

    /** Si el avance acumulado ya cumple el objetivo. Delega en la operación. */
    public boolean estaCompleta(Integer progreso, ProgresoDelDonante donante) {
        return operacion.estaCompleta(progreso, donante);
    }

    /** El valor del atributo que mira esta regla, tomado de la donación. */
    public Object aplicar(ImpactoDonacion donacion) {
        return switch (atributo) {
            case ESTADO -> donacion.getEstado();
            case CATEGORIA -> donacion.getCategoria();
            case CANTIDAD_BIENES -> donacion.getCantidadBienes();
            case SUBCATEGORIA -> donacion.getSubCategoria();
            case ENTIDAD -> donacion.getEntidadBeneficiaria();
            case FECHA -> donacion.getFechaEntrega();
        };
    }

    /** Si esta donación aporta al avance del donante según la operación de la regla. */
    public boolean operar(Object valorAtributo, ProgresoDelDonante donante) {
        return operacion.calcularProgreso(valorAtributo, donante);
    }

    /**
     * Si dos reglas piden exactamente lo mismo (atributo, constancia y operación). Decide si
     * el avance acumulado sigue siendo válido al editar una misión.
     */
    public boolean esEquivalenteA(Regla otra) {
        if (otra == null
                || atributo != otra.atributo
                || operacion == null
                || !operacion.esEquivalenteA(otra.operacion)) {
            return false;
        }
        return esMismaConstancia(otra.constancia);
    }

    /**
     * La constancia se compara campo por campo porque {@code ReglaConstancia} es una entidad
     * sin {@code equals}.
     */
    private boolean esMismaConstancia(ReglaConstancia otra) {
        if (constancia == null || otra == null) {
            return constancia == otra;
        }
        return Objects.equals(constancia.getCantidad(), otra.getCantidad())
                && constancia.getUnidadTiempo() == otra.getUnidadTiempo();
    }
}
