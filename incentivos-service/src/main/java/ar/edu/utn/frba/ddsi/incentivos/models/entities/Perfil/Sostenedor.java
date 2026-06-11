package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;


import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.List;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria.SOSTENEDOR;

public class Sostenedor extends Categoria {
    private static Sostenedor instanciaUnica;
    public Sostenedor(List<Mision> misiones) {
        super(SOSTENEDOR, misiones);
        this.setSiguienteCategoria(Transformador.getInstance());
    }

    public static Sostenedor getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new Sostenedor(); //TODO crear misiones de sostenedor
        }
        return instanciaUnica;
    }
}
