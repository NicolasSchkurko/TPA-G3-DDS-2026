package ar.edu.utn.frba.ddsi.logisticas.controllers;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.CamionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CamionControllerTest {

    @Mock
    private CamionService camionService;

    @InjectMocks
    private CamionController camionController;

    private CamionDTO crearDto(String patente) {
        CamionDTO dto = new CamionDTO();
        dto.setPatente(patente);
        dto.setCapacidadVolumen(30.0);
        dto.setAltura(3.5);
        dto.setCapacidadCarga(5000.0);
        dto.setDisponible(true);
        return dto;
    }

    @Test
    @DisplayName("POST /api/camiones con patente duplicada devuelve 409 Conflict con mensaje descriptivo")
    void crearCamionDuplicadoDevuelveConflict() {
        CamionDTO dto = crearDto("E2E001");
        when(camionService.create(any(CamionDTO.class)))
                .thenThrow(new DuplicateKeyException("E11000 duplicate key error"));

        ResponseEntity<?> response = camionController.crearCamion(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(String.class);
        assertThat((String) response.getBody())
                .contains("Ya existe un camión registrado con la patente: E2E001");
    }

    @Test
    @DisplayName("POST /api/camiones exitoso devuelve 201 Created")
    void crearCamionExitosoDevuelveCreated() {
        CamionDTO dto = crearDto("ABC123");
        when(camionService.create(any(CamionDTO.class))).thenReturn(dto);

        ResponseEntity<?> response = camionController.crearCamion(dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(dto);
    }
}
