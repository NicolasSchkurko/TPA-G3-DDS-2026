package ar.edu.utn.frba.ddsi.incentivos.services;

//intellij q haga los import, estoy con el celu

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class ImpactoDonacionService {
   
//prueba cliente
    private final RestClient restClient;

    // Inyección por constructor
    public PedidoService(RestClient restClient) {
        this.restClient = restClient;
    }

//no se si necesitemos algo asi para recibir 1 donacion o varias donaciones
    public List<AlgoDTO> obtenerListaDeAlgoDeDonacionesServicio() {
        return restClient.get()
            .uri("/algos") // Endpoint de donaciones
            .retrieve()
            .body(new ParameterizedTypeReference<List<AlgoDTO>>() {}); 
            // ParameterizedTypeReference porque el retorno es una lista genérica
    }


public UsuarioDTO obtenerPersonaDeDonacionesPorId(UUID id) {
    return restClient.get()
        .uri("/personas/persona/{id}", id) // Mapea el ID dinámicamente
        .retrieve()
        .body(PerfilDonacionDTO.class); // Retorna un solo objeto directo
}

}
