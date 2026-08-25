package ar.edu.utn.frba.ddsi.incentivos.models.gestores;

import ar.edu.utn.frba.ddsi.incentivos.clients.N8nClient;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPublicacionesPendientes;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class GestorN8N {
    private final N8nClient n8nClient;
    private final RepositorioPublicacionesPendientes repositorio;
    //en un futuro manejare ese repo con una excepcion en n8nClient

    public GestorN8N(N8nClient n8nClient,
                     RepositorioPublicacionesPendientes repositorio) {
        this.n8nClient = n8nClient;
        this.repositorio = repositorio;
    }

    @EventListener
    public void publicarInsignia(MisionCambiada event) {
        n8nClient.publicarInsignia(event.insigniaAnterior(),
                event.misionAnterior(),
                event.nombreUsuario());
    }
}
