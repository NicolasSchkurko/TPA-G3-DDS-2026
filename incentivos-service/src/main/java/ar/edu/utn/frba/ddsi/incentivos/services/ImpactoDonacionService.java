package ar.edu.utn.frba.ddsi.incentivos.services;

//intellij q haga los import, estoy con el celu

import ar.edu.utn.frba.ddsi.incentivos.dto.PerfilDonanteDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class ImpactoDonacionService {
   
//prueba cliente
    private final RestClient generalClient;

    public ImpactoDonacionService(RestClient generalClient) {
        this.generalClient = generalClient;
    }

    public PerfilDonanteDTO obtenerPersonaDeDonacionesPorId(UUID id) {
        return generalClient.get()
            .uri("http://localhost:8081/personas/persona/{id}", id) //url cambia para estender a q servicio se conecta
            .retrieve()
            .body(PerfilDonanteDTO.class);
    }


//no se si necesitemos algo asi para recibir 1 donacion o varias donaciones
//    public List<AlgoDTO> obtenerListaDeAlgoDeDonacionesServicio() {
//        return restClient.get()
//            .uri("/algos") // Endpoint de donaciones
//            .retrieve()
//            .body(new ParameterizedTypeReference<List<AlgoDTO>>() {});
//            // ParameterizedTypeReference porque el retorno es una lista genérica
//    }

}
