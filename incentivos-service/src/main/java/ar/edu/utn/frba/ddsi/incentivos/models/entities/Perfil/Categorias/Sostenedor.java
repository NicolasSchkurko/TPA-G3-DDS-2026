package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias;


import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria.SOSTENEDOR;

public class Sostenedor extends Categoria {
    private static Sostenedor instanciaUnica;

    private Sostenedor(List<Mision> misiones) {
        super(TipoCategoria.SOSTENEDOR, misiones);
    }

    public static Sostenedor getInstance() {
        if (instanciaUnica == null) {
            // TODO: Acá es donde creás y cargás la lista de misiones reales de Sostenedor
            List<Mision> misionesSostenedor = new ArrayList<>();

            instanciaUnica = new Sostenedor(misionesSostenedor);
            instanciaUnica.setSiguienteCategoria(Transformador.getInstance());
        }
        return instanciaUnica;
    }
}
