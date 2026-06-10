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

    public TipoCategoria siguienteTipoCategoria() {
        return nombre.siguiente();
    }

    public Categoria(TipoCategoria nombre, List<Mision> misiones) {
        this.nombre = nombre;
        this.misiones = misiones;
    }

    public boolean esUltimaMision(Mision mision) {
        if (misiones == null || misiones.isEmpty() || mision == null) return false;
        return mision.equals(misiones.get(misiones.size() - 1));
    }

    public Mision siguienteMision(Mision misionActual) {
        if (misiones == null || misiones.isEmpty() || misionActual == null) return null;
        int index = this.misiones.indexOf(misionActual);
        if (index < 0 || index + 1 >= misiones.size()) return null;
        return this.misiones.get(index + 1);
    }
}
