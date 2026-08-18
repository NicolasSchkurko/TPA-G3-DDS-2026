package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.N8nClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class GestorN8N {
    private final N8nClient n8nClient;

    public GestorN8N(N8nClient n8nClient) {
        this.n8nClient = n8nClient;
    }

    @EventListener
    public void publicarInsignia(MisionCambiada event) {
        Insignia insignia = event.perfil().getInsignias().getLast();
        n8nClient.publicarInsignia(new PerfilPublicacionDTO(
                event.perfil().getNombreUsuario(), insignia.getNombre(), insignia.getDescripcion()));
    }
}
