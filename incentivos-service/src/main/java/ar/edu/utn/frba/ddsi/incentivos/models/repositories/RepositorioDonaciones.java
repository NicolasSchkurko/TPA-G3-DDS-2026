package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Donacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.ArrayList;
import java.util.List;

public class RepositorioDonaciones {
    private static RepositorioDonaciones instanciaUnica;

    private final List<Donacion> donaciones;

    private RepositorioDonaciones() {
        this.donaciones = new ArrayList<>();
    }

    public static RepositorioDonaciones getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioDonaciones();
        }
        return instanciaUnica;
    }
}
