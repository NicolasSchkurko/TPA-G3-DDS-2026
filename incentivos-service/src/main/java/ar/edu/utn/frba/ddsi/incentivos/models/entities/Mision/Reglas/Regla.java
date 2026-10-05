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
 * Qué tiene que cumplir el donante para completar una misión: sobre qué atributo de la
 * donación se lo mide ({@link AtributoImpacto}), con qué operación, y qué condición de
 * tiempo tiene que cumplirse.
 *
 * <p>No tiene setters: la regla se construye entera con la misión, y después solo se
 * reemplaza por otra cuando el admin cambia el criterio (punto 15).
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

    // Ni constancia ni operacion llevan orphanRemoval (punto 17), a diferencia de
    // Mision.reglaDeProgreso y de Perfil.progresoMisionActual. El motivo es que la regla no
    // se modifica nunca en el lugar: cuando el admin cambia el criterio de completado,
    // Mision.actualizar reemplaza la Regla entera por una nueva. Y como esta relacion tiene
    // cascade = ALL, que incluye REMOVE, borrar la regla vieja se lleva por delante su
    // constancia y su operacion. No queda huerfana ninguna de las dos.
    //
    // Poner orphanRemoval aca seria ademas redundante y peligroso: si alguien llegara a
    // reasignar constancia u operacion sobre una regla que ya se guardo, Hibernate borraria
    // la fila sin comprobar que nadie mas la este usando.

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

    /**
     * Si el avance acumulado ya cumple el objetivo.
     *
     * <p>Es una delegación a la operación: la regla no sabe contar, solo le pasa lo que
     * tiene.
     */
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
     * Si dos reglas piden exactamente lo mismo al donante. Es lo que decide si el avance
     * acumulado sigue siendo válido al editar una misión (punto 15).
     *
     * <p>Una regla es el atributo que se mira, la constancia y la operación. Si las tres
     * coinciden, lo que el donante viene acumulando sigue siendo válido y no hay por qué
     * reiniciar nada. Editar el nombre, la descripción o la insignia de la misión no
     * cambia nada de esto, y por eso esos cambios ahora conservan el avance de todos los
     * que estaban en la misión.
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
     * La constancia se compara campo por campo y no con {@code equals} porque
     * {@code ReglaConstancia} es una entidad y no tiene {@code equals}: sin esto, dos
     * objetos distintos darían siempre "cambiaron".
     */
    private boolean esMismaConstancia(ReglaConstancia otra) {
        if (constancia == null || otra == null) {
            return constancia == otra;
        }
        return Objects.equals(constancia.getCantidad(), otra.getCantidad())
                && constancia.getUnidadTiempo() == otra.getUnidadTiempo();
    }
}
