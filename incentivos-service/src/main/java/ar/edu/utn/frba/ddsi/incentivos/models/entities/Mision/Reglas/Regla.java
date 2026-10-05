package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.ProgresoDelDonante;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Regla {
    //que sea capaz de hacer una mision tipo:
//hacer x cantidad de x tipo de donaciones por x cant de tiempo
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idRegla;

    @OneToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "constancia_id")
    private ReglaConstancia constancia; //puede ser null

    @Enumerated(EnumType.STRING)
    private AtributoImpacto atributo; //atributo de ImpactoDonacion

    @OneToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "operacion_id", nullable = false)
    private Operacion operacion; //define relacion entre atributo y lista donaciones

    public Regla(
            ReglaConstancia constancia,
            AtributoImpacto atributo,
            Operacion operacion
    ) {
        this.constancia = constancia;
        this.atributo = atributo;
        this.operacion = operacion;
    }

    public Boolean estaCompleta(Integer progreso, ProgresoDelDonante donante) {
        return operacion.estaCompleta(progreso, donante);
    }

    public Object aplicar(ImpactoDonacion donacion){
        return switch (atributo) {
            case ESTADO -> donacion.getEstado();
            case CATEGORIA -> donacion.getCategoria();
            case CANTIDAD_BIENES -> donacion.getCantidadBienes();
            case SUBCATEGORIA -> donacion.getSubCategoria();
            case ENTIDAD -> donacion.getEntidadBeneficiaria();
            case FECHA -> donacion.getFechaEntrega();
        };
    }

    public Boolean operar(Object valorAtributo, ProgresoDelDonante donante){
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
