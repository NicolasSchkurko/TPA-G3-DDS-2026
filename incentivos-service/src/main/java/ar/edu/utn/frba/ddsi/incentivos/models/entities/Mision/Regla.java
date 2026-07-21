package ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision;

import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Regla {
    //que sea capaz de hacer una mision tipo:
//hacer x cantidad de donaciones de categorias diferentes por x cant de tiempo x
    private ReglaConstancia constancia;
    private Function<ImpactoDonacion, ?> atributo; //atributo de ImpactoDonacion
    private Operacion operacion; //define relacion entre atributo y lista donaciones

    void aplicar(ImpactoDonacion donacion, Mision mision);

    void evaluarProgreso(Mision mision);

    Integer conseguirProgreso(List<ImpactoDonacion> donacionesExitosas);
}
