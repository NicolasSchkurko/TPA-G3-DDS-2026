package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.DestinoEntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.PeticionEntregaDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogisticaService {
    // private final Repo repo;

//    public LogisticaService(Repo repo) {
//        this.repo = repo;
//    }

    public List<DestinoEntregaDTO> procesarPeticion(PeticionEntregaDTO request){
        //lista de donaciones: tiene entidadBeneficiaria(direccion), lista de bienes
    }
}
