package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Setter;
import lombok.Getter;
import org.springframework.stereotype.Repository;

@Getter
@Setter
@Repository
public class RepositorioDonaciones {
    private final List<ImpactoDonacion> donaciones;

    public RepositorioDonaciones() {
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

    public ImpactoDonacion buscarDonacionPorIDs(UUID idUsuario, UUID idDonacion) {
        if (idDonacion == null || idUsuario == null || donaciones.isEmpty()) {
            return null;
        }

        return donaciones.stream()
                .filter(d -> idUsuario.equals(d.getIdUsuario()) && idDonacion.equals(d.getIdDonacion()))
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
}