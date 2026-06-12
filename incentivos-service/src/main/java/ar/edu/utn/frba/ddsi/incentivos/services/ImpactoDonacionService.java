package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.ImpactoDonacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ImpactoDonacion;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ImpactoDonacionService {
    //este es el client *gritos internos*
    private final RestTemplate restTemplate = new RestTemplate();

    public ImpactoDonacionDTO buscarDonacionPorUUID(UUID id) {
        return restTemplate.getForObject(
                "http://localhost:8080/donaciones/"
                        + id,
                ImpactoDonacionDTO.class
        );
    }


}
