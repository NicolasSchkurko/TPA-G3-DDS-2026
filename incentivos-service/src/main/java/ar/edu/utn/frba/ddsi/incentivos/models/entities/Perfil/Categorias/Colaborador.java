package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria.*;


import java.util.ArrayList;
import java.util.List;

public class Colaborador extends Categoria {
    private static Colaborador instanciaUnica;

    private Colaborador(List<Mision> misiones) {
        super(TipoCategoria.COLABORADOR, misiones);
    }

    public static Colaborador getInstance() {
        if (instanciaUnica == null) {
            List<Mision> misionesColaborador = new ArrayList<>();
            // TODO: Acá es donde creás y cargás la lista de misiones q le correspondenr
            // Ejemplo: misionesColaborador.add(new DonacionesExitosa("Iniciación", insignia, 3));
            instanciaUnica = new Colaborador(misionesColaborador);

            instanciaUnica.setSiguienteCategoria(Sostenedor.getInstance());
        }
        return instanciaUnica;
    }
}