package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.IncentivosDonacionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
@Service

public class IncentivosClient {

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    private final RestTemplate restTemplate;

    public IncentivosClient(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
    }

    public void peticionCrearPerfil(IDDTO dto) {
      restTemplate.postForEntity(
          incentivosUrl,
          dto,
          void.class
      );
    }

    public void notificarDonacionAsignada(IncentivosDonacionDTO dto) {
        restTemplate.postForEntity(
                incentivosUrl + dto.getIdUsuario() + "/perfil",
                dto,
                void.class
        );
    }
}

