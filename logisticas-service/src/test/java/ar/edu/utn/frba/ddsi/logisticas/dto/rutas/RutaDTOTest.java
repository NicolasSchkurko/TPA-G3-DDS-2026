package ar.edu.utn.frba.ddsi.logisticas.dto.rutas;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Punto 73: la ruta expone las coordenadas de sus paradas")
class RutaDTOTest {

    @Test
    @DisplayName("RutaDTO conserva latitud y longitud para que el front pueda unir los puntos")
    void exponeLasCoordenadasDeCadaParada() {
        Direccion direccion = new Direccion("Calle", null, 100, null, null,
                "CABA", "CABA", "Argentina", -34.6037, -58.3816);
        Entidad entidad = new Entidad(UUID.randomUUID(), direccion);

        Ruta ruta = new Ruta(null);
        Parada parada = new Parada();
        parada.setEntidadDestino(entidad);
        parada.setDireccion(direccion);
        parada.agregarItem(new ItemEntrega(UUID.randomUUID(), 1,
                UnidadDeMedida.UNIDADES, entidad));
        ruta.getParadas().add(parada);

        RutaDTO dto = new RutaDTO().aplicar(ruta);

        assertThat(dto.getParadas()).hasSize(1);
        assertThat(dto.getParadas().get(0).getDireccion().getLatitud()).isEqualTo(-34.6037);
        assertThat(dto.getParadas().get(0).getDireccion().getLongitud()).isEqualTo(-58.3816);
    }
}
