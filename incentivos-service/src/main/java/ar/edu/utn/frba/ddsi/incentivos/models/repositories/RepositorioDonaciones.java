package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Setter;
import lombok.Getter;

@Getter
@Setter

public class RepositorioDonaciones {
    private final List<ImpactoDonacion> donaciones;

    private RepositorioDonaciones() {
        this.donaciones = new ArrayList<>();
    }

    public void guardar(ImpactoDonacion donacion) {
        if (donacion != null && !donaciones.contains(donacion)) {
            donaciones.add(donacion);
        }
    }

    public void eliminarDonacion(ImpactoDonacion donacion) {
        donaciones.remove(donacion);
    }

    public List<ImpactoDonacion> listarTodas() {
        return List.copyOf(donaciones);
    }

    public ImpactoDonacion buscarPorIDDonacion(UUID id) {
        if (id == null || donaciones.isEmpty()) {
            return null;
        }

        return donaciones.stream()
                .filter(d -> id.equals(d.getIdDonacion()))
                .findFirst()
                .orElse(null);
    }

    public ImpactoDonacion buscarPorIDUsuario(UUID id) {
        if (id == null || donaciones.isEmpty()) {
            return null;
        }
        return donaciones.stream()
                .filter(donacion -> id.equals(donacion.getIdUsuario()))
                .findFirst()
                .orElse(null);
    }

    public List<ImpactoDonacion> buscarDonacionesPorIDUsuario (UUID id){
        if (id == null || donaciones.isEmpty()) {
            return null;
        }
        return donaciones.stream()
                .filter(d -> d.getIdUsuario().equals(id))
                .toList();
    }

    public void actualizar(Perfil perfil) {
        if (perfil == null || perfil.getMisionActual() == null) {
            return;
        }
        List<ImpactoDonacion> exitosas = perfil.getMisionActual().getDonacionesExitosas();
        if (exitosas == null || exitosas.isEmpty()) {
            return;
        }
        this.guardar(exitosas.getLast());
    }
}