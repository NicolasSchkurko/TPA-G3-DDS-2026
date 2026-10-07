package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Cola en memoria de publicaciones que no se pudieron enviar a n8n. Es un buffer transitorio:
 * se pierde al reiniciar y no se comparte entre réplicas.
 */
@Repository
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
