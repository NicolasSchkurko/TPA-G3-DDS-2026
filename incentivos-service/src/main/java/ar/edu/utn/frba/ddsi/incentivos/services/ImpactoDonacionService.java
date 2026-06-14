package ar.edu.utn.frba.ddsi.incentivos.services;

//intellij q haga los import, estoy con el celu

import org.springframework.stereotype.Service;

@Service
public class ImpactoDonacionService {
   
//prueba cliente
    private final RestClient generalClient;

    public UsuarioClientService(RestClient generalClient) {
        this.generalClient = generalClient;
    }

    public PerfilDonacionDTO obtenerPersonaDeDonacionesPorId(UUID id) {
        return generalClient.get()
            .uri("http://localhost:8081/personas/persona/{id}", id) //url cambia para estender a q servicio se conecta
            .retrieve()
            .body(PerfilDonacionDTO.class);
    }


//no se si necesitemos algo asi para recibir 1 donacion o varias donaciones
    public List<AlgoDTO> obtenerListaDeAlgoDeDonacionesServicio() {
        return restClient.get()
            .uri("/algos") // Endpoint de donaciones
            .retrieve()
            .body(new ParameterizedTypeReference<List<AlgoDTO>>() {}); 
            // ParameterizedTypeReference porque el retorno es una lista genérica
    }

}
