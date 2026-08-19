package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;

import java.util.List;
import java.util.UUID;

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

    public List<ItemEntrega> buscarItems(List<UUID> todosLosIdsItems){
        return todosLosIdsItems.stream().map(this::buscarItem).toList();
    }

    public void eliminarItem(UUID id){
        if (repoItemEntrega.findById(id) == null) {
            throw new IllegalArgumentException("Entrega no encontrada");
        }
        repoItemEntrega.deleteById(id);
    }

    public void guardarItem(ItemEntrega item){
        repoItemEntrega.save(item);
    }

    public List<ItemEntrega> buscarNoRecibidos(){
        return repoItemEntrega.findByEstado(EstadoEntrega.NO_RECIBIDA);
    }

    public List<ItemEntrega> buscarPendientes(){
        return repoItemEntrega.findByEstado(EstadoEntrega.PENDIENTE);
    }
}