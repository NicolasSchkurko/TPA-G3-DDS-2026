package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

@Repository
public class RepositorioActividades {
    private final List<HistorialActividad> historial;

    public RepositorioActividades() {
        this.historial = new ArrayList<>();
    }

    public void eliminarActividad(HistorialActividad actividad) {
        historial.remove(actividad);
    }

    public List<HistorialActividad> listarTodas() {
        return List.copyOf(historial);
    }

    public HistorialActividad buscarPorIdPerfil(UUID idPerfil) {
        if (idPerfil == null) return null;
        return historial.stream()
                .filter(actividad -> idPerfil.equals(actividad.getIdPerfil()))
                .findFirst()
                .orElse(null);
    }

/*
    public ImpactoDonacion buscarDonacionPorIDs(UUID idPerfil, UUID idDonacion) {
        HistorialActividad actividad = buscarPorIdPerfil(idPerfil);
        if (idDonacion == null || actividad == null) {
            return null;
        }
        return actividad.getActividadPorMes().stream()
                .flatMap(mes -> mes.getDonacionesEnMes().stream())
                .filter(d -> idDonacion.equals(d.getIdDonacion()))
                .findFirst().orElse(null);
    }

    public List<ImpactoDonacion> buscarDonacionesPorIDPerfil(UUID id){
        HistorialActividad actividad = buscarPorIdPerfil(id);
        if (actividad == null) {
            return List.of();
        }
        return actividad.getActividadPorMes().stream()
                .flatMap(mes -> mes.getDonacionesEnMes().stream())
                .toList();
    }
*/

    public void guardarDonacion(UUID idPerfil, ImpactoDonacion donacion) {
        if (idPerfil == null || donacion == null) {
            return;
        }

        HistorialActividad actividad = buscarPorIdPerfil(idPerfil);
        if (actividad == null) {
            actividad = new HistorialActividad(idPerfil, new ArrayList<>());
        }
        actividad.agregarDonacion(donacion);

        HistorialActividad finalActividad = actividad;
        historial.removeIf(actual -> finalActividad.getIdPerfil().equals(actual.getIdPerfil()));
        historial.add(actividad);
    }
}
