package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class GestorItemEntrega {
    private final RepositorioItemEntrega repoItemEntrega;

    public GestorItemEntrega(RepositorioItemEntrega repoItemEntrega){
        this.repoItemEntrega = repoItemEntrega;
    }

    public List<ItemEntrega> listarItems(){
        return repoItemEntrega.findAll();
    }

    public ItemEntrega buscarItem(UUID id){
        ItemEntrega item = repoItemEntrega.findById(id);
        if (item == null) throw new IllegalArgumentException("Entrega no encontrada");
        return item;
    }

    public List<ItemEntrega> buscarItems(List<UUID> idsItems){
        return idsItems.stream().map(this::buscarItem).toList();
    }

    public List<ItemEntrega> buscarNoRecibidos(){
        return repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA);
    }

    public List<ItemEntrega> buscarPendientes(){
        return repoItemEntrega.findByEstado(EstadoEntrega.PENDIENTE);
    }

    public void guardarItem(ItemEntrega item){
        repoItemEntrega.save(item);
    }

    public void eliminarItem(UUID id){
        if (repoItemEntrega.findById(id) == null) {
            throw new IllegalArgumentException("Entrega no encontrada");
        }
        repoItemEntrega.deleteById(id);
    }
}