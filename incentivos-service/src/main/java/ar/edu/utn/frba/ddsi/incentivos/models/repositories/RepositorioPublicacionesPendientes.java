package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Cola en memoria de publicaciones que no se pudieron enviar a n8n.
 *
 * <p>Es solo un buffer transitorio: se pierde al reiniciar el servicio y no se
 * comparte entre replicas. El reemplazo por una tabla de outbox con scheduler
 * de reintento esta trackeado en {@code PENDIENTES.md} (punto 3).
 */
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
