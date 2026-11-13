package ar.edu.utn.frba.ddsi.donaciones.bien;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.BienResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.UnidadDeMedida;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnidadDeMedidaTest {

    @Test
    void mapeaUnidadesAdemasDePesoYVolumen() {
        BienResumenDTO dto = new BienResumenDTO();
        dto.setDescripcion("Frazadas");
        dto.setCantidad(10);
        dto.setUnidadDeMedida("UNIDADES");
        dto.setTipoBien("CON_ESTADO");

        Bien bien = dto.toDomain();

        assertThat(bien.getUnidadUtilizada()).isEqualTo(UnidadDeMedida.UNIDADES);
    }
}
