package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Categoria {

    private TipoCategoria nombre;
    private Categoria siguienteCategoria;
    private List<Mision> misiones;

    public Categoria(TipoCategoria nombre, List<Mision> misiones) {
        this.nombre = nombre;
        this.misiones = misiones;
    }

    public boolean esUltimaMision(Mision mision) {
        return mision.equals(
                misiones.getLast()
        );
    }

    public Mision siguienteMision(Mision misionActual) {
        Mision nuevaMision = misionActual;
        int index = this.misiones.indexOf(misionActual);
        nuevaMision = this.misiones.get(index + 1);

        return nuevaMision;
    }
}
