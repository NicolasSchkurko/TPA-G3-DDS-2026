package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Categoria {
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
        return mision.equals(misiones.getLast());
    }

    public Mision primeraMision(){
        if (misiones == null || misiones.isEmpty()) return null;
        return misiones.getFirst();
    }

    public Mision siguienteMision(Mision misionActual) {
        if (misiones == null || misiones.isEmpty() || misionActual == null) return null;
        int index = this.misiones.indexOf(misionActual);
        if (index < 0 || index + 1 >= misiones.size()) return null;
        return this.misiones.get(index + 1);
    }
}

