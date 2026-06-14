package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria.TRANSFORMADOR;

public class Transformador extends Categoria {
    private static Transformador instanciaUnica;

    private Transformador(List<Mision> misiones) {
        super(TipoCategoria.TRANSFORMADOR, misiones);
    }

    public static Transformador getInstance() {
        if (instanciaUnica == null) {
            // TODO: Acá es donde creás y cargás la lista de misiones reales de Transformador
            List<Mision> misionesTransformador = new ArrayList<>();

            instanciaUnica = new Transformador(misionesTransformador);

            instanciaUnica.setSiguienteCategoria(null);
        }
        return instanciaUnica;
    }
}
