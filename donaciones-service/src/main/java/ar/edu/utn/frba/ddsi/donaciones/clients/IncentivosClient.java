package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.ActividadDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.UUID;
@Service

public class IncentivosClient {

    @Value("${servicio.incentivos.url}")
    private String incentivosUrl;

    private final RestTemplate restTemplate;

    public IncentivosClient(RestTemplate restTemplate) {
      this.restTemplate = restTemplate;
    }

    public ActividadDTO obtenerDiasDeInactividad(UUID idUsuario) {
      return restTemplate.getForObject(
          incentivosUrl + idUsuario + "/metricas",
          ActividadDTO.class
      );
    }
  }

