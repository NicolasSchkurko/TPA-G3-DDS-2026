package ar.edu.utn.frba.ddsi.logisticas.dto.camion;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CamionDTO")
class CamionDTOTest {

    @Test
    @DisplayName("toDomain: sin 'disponible' queda disponible por defecto")
    void sinDisponibleQuedaDisponible() {
        Camion camion = dtoConDisponible(null).toDomain();
        assertTrue(camion.getDisponible());
    }

    @Test
    @DisplayName("toDomain: 'disponible' false se respeta")
    void disponibleFalseSeRespeta() {
        Camion camion = dtoConDisponible(false).toDomain();
        assertFalse(camion.getDisponible());
    }

    @Test
    @DisplayName("aplicar: copia camión y chofer")
    void aplicarCopiaCamionYChofer() {
        Chofer chofer = new Chofer(UUID.randomUUID(), "Ana");
        Camion camion = new Camion(chofer, "AAA111", 10.0, 3.0, 1000.0, false);

        CamionDTO dto = new CamionDTO().aplicar(camion);

        assertThat(dto.getPatente()).isEqualTo("AAA111");
        assertThat(dto.getIdChofer()).isEqualTo(chofer.getIdChofer());
        assertThat(dto.getCapacidadVolumen()).isEqualTo(10.0);
        assertThat(dto.getAltura()).isEqualTo(3.0);
        assertThat(dto.getCapacidadCarga()).isEqualTo(1000.0);
        assertThat(dto.getDisponible()).isFalse();
    }

    @Test
    @DisplayName("aplicar: camión sin chofer deja idChofer en null")
    void aplicarSinChoferDejaIdChoferNull() {
        Camion camion = new Camion("AAA111", 10.0, 3.0, 1000.0, true);

        CamionDTO dto = new CamionDTO().aplicar(camion);

        assertThat(dto.getIdChofer()).isNull();
        assertNotNull(dto.getPatente());
    }

    private CamionDTO dtoConDisponible(Boolean disponible) {
        CamionDTO dto = new CamionDTO();
        dto.setPatente("AAA111");
        dto.setCapacidadVolumen(10.0);
        dto.setAltura(3.0);
        dto.setCapacidadCarga(1000.0);
        dto.setDisponible(disponible);
        return dto;
    }
}
