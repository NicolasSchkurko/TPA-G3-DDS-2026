package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria.*;
import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria.*;

public class Perfil {
    //TODO agregar los atributos necesarios que vengan del repositorio
    //private PersonaDonante persona;
    private Categoria categoriaActual;
    private List<Insignia> insignias;
    private Mision misionActual;

    public Perfil(){
  //      this.persona = persona;
        this.categoriaActual = new Categoria(COLABORADOR, new ArrayList<>());
        this.insignias = new ArrayList<>();
  //      this.misionActual = ;
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
