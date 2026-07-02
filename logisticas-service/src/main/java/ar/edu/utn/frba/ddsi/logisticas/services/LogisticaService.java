package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.*;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LogisticaService {
    private final RepositorioCamiones repositorioCamiones;
    private final RepositorioItemEntrega repositorioItemEntrega;

    public LogisticaService(RepositorioCamiones repositorioCamiones,
                            RepositorioItemEntrega repositorioItemEntrega) {
        this.repositorioCamiones = repositorioCamiones;
        this.repositorioItemEntrega = repositorioItemEntrega;
    }
    int index = perfiles.indexOf(existente);
            if (index >= 0) {
        perfiles.set(index, existente);
    }

    public List<DestinoEntregaDTO> procesarPeticion(PeticionEntregaDTO request){
        List<EntregaDTO> entregas = request.getEntregas();
        List<DestinoEntregaDTO> destinos = new ArrayList<>();
        for(int i = 0; i < entregas.size(); i++){
            EntregaDTO entregaActual = entregas.get(i);
            List<BienDTO> bienes = entregaActual.getDonacionResumen();
            for(int j= 0; j < bienes.size(); j++){
                BienDTO bien = bienes.get(j);
                repositorioItemEntrega.save(new ItemEntrega((entregaActual.getIdsDonaciones().get(j), bien.getCantidad(), bien.getUnidadDeMedida(), entregaActual.getEntidadBeneficiaria()));
            }
            DestinoEntregaDTO destinoDTO = new DestinoEntregaDTO();
            destinos.add(destinoDTO);
        }
        return destinos;
    }

    public Camion convertirDTO(CamionDTO dto){
        return new Camion(dto.getPatente(), dto.getCapacidadVolumen(), dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
    }

    public CamionDTO convertirACamionDTO(Camion camion){
        CamionDTO dto = new CamionDTO();
        dto.setPatente(camion.getPatente());
        dto.setCapacidadVolumen(camion.getCapacidadVolumen());
        dto.setAltura(camion.getAltura());
        dto.setCapacidadCarga(camion.getCapacidadCarga());
        dto.setDisponible(camion.getDisponible());
        return dto;
    }

    public void guardarCamiones(List<Camion> camiones){
        this.repositorioCamiones.addAll(camiones);
    }
}
