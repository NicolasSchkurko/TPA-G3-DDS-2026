package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioCategorias;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria.*;


import java.util.List;

public class Colaborador extends Categoria {
    private static Colaborador instanciaUnica;
    public Colaborador(List<Mision> misiones) {
        super(COLABORADOR, misiones);
        this.setSiguienteCategoria(Sostenedor.getInstance());
    }

    public static Colaborador getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new Colaborador(); //TODO crear misiones de colaborador
        }
        return instanciaUnica;
    }
}