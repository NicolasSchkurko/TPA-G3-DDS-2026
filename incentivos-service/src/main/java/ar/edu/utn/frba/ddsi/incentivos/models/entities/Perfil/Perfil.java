package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria.*;
import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria.*;

@Getter
@Setter
public class Perfil {
    //TODO agregar los atributos necesarios que vengan del repositorio
    private String nombreUsuario;
    private Categoria categoriaActual;
    private List<Insignia> insignias;
    private Mision misionActual;

    public Perfil(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = new Categoria(COLABORADOR, new ArrayList<>());
        this.insignias = new ArrayList<>();
    }

    public void ascenderCategoria() {
        categoriaActual = categoriaActual.getSiguienteCategoria();
    }

    private void misionCompletada(){
        if(misionActual.completarMision(this)){
            if (categoriaActual.esUltimaMision(misionActual)) {
                this.ascenderCategoria();
            }
            else{
                misionActual = categoriaActual.siguienteMision(misionActual);
            }
        }
    }
}
