package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Categoria {
    private TipoCategoria nombre;
    private TipoCategoria siguienteCategoria;
    private List<Mision> misiones;

    public Categoria(TipoCategoria nombre, TipoCategoria siguienteCategoria) {
        this.nombre = nombre;
        this.siguienteCategoria = siguienteCategoria;
        this.misiones = new ArrayList<>();

    }

    public void agregarMision(Mision mision) {
        this.misiones.add(mision);
    }

    public void eliminarMision(Mision mision) {
        this.misiones.remove(mision);
    }

    public boolean esUltimaMision(Mision mision) {
        if (misiones == null || misiones.isEmpty() || mision == null) return false;
        return mision.equals(misiones.getLast());
    }

    public Mision primeraMision() {
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