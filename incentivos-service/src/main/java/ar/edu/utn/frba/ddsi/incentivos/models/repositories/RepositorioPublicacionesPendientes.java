package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilPublicacionDTO;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
//guardar las publicaciones y es decision de la empresa pensar que hacer con ellas
//al fallar la publicacion en redes sociales
public class RepositorioPublicacionesPendientes {
    private final List<PerfilPublicacionDTO> pendientes;

    public RepositorioPublicacionesPendientes() {
        this.pendientes = new ArrayList<>();
    }

    public void guardar(PerfilPublicacionDTO pendiente) {
        if (pendiente != null && !pendientes.contains(pendiente)) {
            pendientes.add(pendiente);
        }
    }

    public void eliminar(PerfilPublicacionDTO pendiente) {
        pendientes.remove(pendiente);
    }

    public List<PerfilPublicacionDTO> listarTodas() {
        return List.copyOf(pendientes);
    }
}
