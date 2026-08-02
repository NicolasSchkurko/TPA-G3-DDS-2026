package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import java.util.function.Function;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorOperacion.Operacion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Regla {
    //que sea capaz de hacer una mision tipo:
//hacer x cantidad de x tipo de donaciones por x cant de tiempo
    private ReglaConstancia constancia;
    private Function<ImpactoDonacion, ?> atributo; //atributo de ImpactoDonacion
    private Operacion operacion; //define relacion entre atributo y lista donaciones

    public Regla(
            ReglaConstancia constancia,
            Function<ImpactoDonacion, ?> atributo,
            Operacion operacion
    ) {
        this.constancia = constancia;
        this.atributo = atributo;
        this.operacion = operacion;
    }

    public Boolean estaCompleta(Integer progreso) {
        return operacion.estaCompleta(progreso);
    }

    public Object aplicar(ImpactoDonacion donacion){
        return atributo.apply(donacion);
    }

    public Boolean operar(Object valorAtributo){
        return operacion.calcularProgreso(valorAtributo);
    }
}
