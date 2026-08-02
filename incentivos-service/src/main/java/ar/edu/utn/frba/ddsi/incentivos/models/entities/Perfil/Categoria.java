package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Categoria {
    private UUID idCategoria; // id interno
    private String nombre;
    private Integer posicionSecuencia;
    private List<Mision> misiones;

    public Categoria(String nombre,
                     Integer posicionSecuencia,
                     List<Mision> misiones) {
        this.idCategoria = UUID.randomUUID();
        this.nombre = nombre;
        this.posicionSecuencia = posicionSecuencia;
        this.misiones = misiones;
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

    public CategoriaDTO toDTO(){
        List<String> nomMisiones = misiones.stream()
                .map(Mision::getNombreMision).toList();

        return new CategoriaDTO(
                nombre, posicionSecuencia, nomMisiones
        );
    }
}