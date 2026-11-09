package ar.edu.utn.frba.ddsi.logisticas.dto.chofer;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ChoferDTO")
class ChoferDTOTest {

    @Test
    @DisplayName("toDomain: sin 'disponible' queda disponible y genera id")
    void sinDisponibleQuedaDisponible() {
        ChoferDTO dto = new ChoferDTO();
        dto.setNombre("Juan");

        Chofer chofer = dto.toDomain();

        assertNotNull(chofer.getIdChofer());
        assertTrue(chofer.isDisponible());
        assertThat(chofer.getNombre()).isEqualTo("Juan");
    }

    @Test
    @DisplayName("toDomain: 'disponible' false se respeta")
    void disponibleFalseSeRespeta() {
        ChoferDTO dto = new ChoferDTO();
        dto.setNombre("Juan");
        dto.setDisponible(false);

        assertFalse(dto.toDomain().isDisponible());
    }

    @Test
    @DisplayName("aplicar: copia los datos del chofer")
    void aplicarCopiaLosDatos() {
        Chofer chofer = new Chofer(UUID.randomUUID(), "Ana", false);

        ChoferDTO dto = new ChoferDTO().aplicar(chofer);

        assertThat(dto.getIdChofer()).isEqualTo(chofer.getIdChofer());
        assertThat(dto.getNombre()).isEqualTo("Ana");
        assertThat(dto.getDisponible()).isFalse();
        assertThat(dto.getActivo()).isTrue();
    }
}
