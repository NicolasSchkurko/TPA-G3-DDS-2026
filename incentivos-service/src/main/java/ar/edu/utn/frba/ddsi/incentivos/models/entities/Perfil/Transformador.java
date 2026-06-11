package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.List;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria.TRANSFORMADOR;

public class Transformador extends Categoria{
    private static Transformador instanciaUnica;
    public Transformador(List<Mision> misiones) {
        super(TRANSFORMADOR, misiones);
        this.setSiguienteCategoria(Transformador.getInstance());
    }

    public static Transformador getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new Transformador(); //TODO crear misiones de transformador
        }
        return instanciaUnica;
    }
}
