package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RepositorioDonaciones {
    private static RepositorioDonaciones instanciaUnica;
    private final List<ImpactoDonacion> donaciones;

    private RepositorioDonaciones() {
        this.donaciones = new ArrayList<>();
    }

    public static RepositorioDonaciones getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new RepositorioDonaciones();
        }
        return instanciaUnica;
    }

    public void guardar(ImpactoDonacion donacion) {
        if (donacion != null && !donaciones.contains(donacion)) {
            donaciones.add(donacion);
        }
    }

    public List<ImpactoDonacion> listarTodas() {
        return List.copyOf(donaciones);
    }

    public ImpactoDonacion buscarPorIDDonacion(UUID id) {
        if (id == null) return null;
        return donaciones.stream()
                .filter(d -> id.equals(d.getIdDonacion()))
                .findFirst()
                .orElse(null);
    }
}