package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.HistorialActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioActividades;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
public record GestorActividad(RepositorioActividades repositorio) {
    public void guardarActividad(HistorialActividad actividad){
        repositorio.guardar(actividad);
    }

    public HistorialActividad buscarActividadPorPerfil(UUID idPerfil) {
        return repositorio.buscarPorIdPerfil(idPerfil);
    }

    /** Registra la donacion en el historial del perfil, creandolo si es necesario. */
    public void guardarDonacion(UUID idPerfil, ImpactoDonacion donacion) {
        if (idPerfil == null || donacion == null) {
            return;
        }

        HistorialActividad actividad = buscarActividadPorPerfil(idPerfil);
        if (actividad == null) {
            actividad = new HistorialActividad(idPerfil, new ArrayList<>());
        }
        actividad.agregarDonacion(donacion);
        guardarActividad(actividad);
    }

}
