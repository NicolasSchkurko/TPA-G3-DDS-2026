package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public class RepositorioImpactos {
    private final List<ImpactoDonacion> impactos;

    public RepositorioImpactos() {
        this.impactos = new ArrayList<>();
    }

    public void guardar(ImpactoDonacion donacion) {
        if (donacion != null && !impactos.contains(donacion)) {
            impactos.add(donacion);
        }
    }

    public void eliminarDonacion(ImpactoDonacion donacion) {
        impactos.remove(donacion);
    }

    public List<ImpactoDonacion> listarTodas() {
        return List.copyOf(impactos);
    }

    public ImpactoDonacion buscarDonacionPorIDs(UUID idUsuario, UUID idDonacion) {
        if (idDonacion == null || idUsuario == null || impactos.isEmpty()) {
            return null;
        }

        return impactos.stream()
                .filter(d -> idUsuario.equals(d.getIdUsuario()) && idDonacion.equals(d.getIdDonacion()))
                .findFirst()
                .orElse(null);
    }

    public List<ImpactoDonacion> buscarDonacionesPorIDUsuario(UUID id){
        if (id == null || impactos.isEmpty()) {
            return null;
        }
        return impactos.stream()
                .filter(d -> d.getIdUsuario().equals(id))
                .toList();
    }
}