package ar.edu.utn.frba.ddsi.donaciones.clients;

import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
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

    public void peticionCrearPerfil(IDDTO dto) {
        try {
            restTemplate.postForEntity(incentivosUrl, dto, Void.class);
        } catch (Exception e) {
            System.err.println("Fallo al crear perfil en incentivos: " + e.getMessage());
        }
    }

    public void notificarDonacionAsignada(UUID idUsuario, IncentivosDonacionDTO dto) {
        try {
            restTemplate.postForEntity(incentivosUrl + idUsuario, dto, Void.class);
        } catch (Exception e) {
            System.err.println("Fallo al notificar donación asignada a incentivos: " + e.getMessage());
        }
    }
}