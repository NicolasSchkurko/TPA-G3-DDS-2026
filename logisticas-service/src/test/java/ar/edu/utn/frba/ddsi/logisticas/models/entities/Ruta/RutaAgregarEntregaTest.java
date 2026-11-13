package ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RutaAgregarEntregaTest {

    @Test
    @DisplayName("agregarEntrega debe agrupar dos donaciones destinadas a la misma entidad en una sola parada")
    void agregarEntregaAgrupaPorMismaEntidadDestinoConInstanciasDistintas() {
        Camion camion = new Camion("ABC123", 1000.0, 50.0, 5000.0, true);
        Ruta ruta = new Ruta(camion);

        UUID idEntidad = UUID.randomUUID();
        Direccion direccion = new Direccion();

        // Dos instancias diferentes de Entidad en memoria con el mismo idEntidadBeneficiaria (como ocurre al deserializar desde Mongo)
        Entidad entidad1 = new Entidad(idEntidad, direccion);
        Entidad entidad2 = new Entidad(idEntidad, direccion);

        ItemEntrega item1 = new ItemEntrega(UUID.randomUUID(), 10, UnidadDeMedida.KILOGRAMOS, entidad1);
        ItemEntrega item2 = new ItemEntrega(UUID.randomUUID(), 20, UnidadDeMedida.KILOGRAMOS, entidad2);

        ruta.agregarEntrega(item1);
        ruta.agregarEntrega(item2);

        assertEquals(1, ruta.getParadas().size(), "Debe existir exactamente 1 parada consolidada para la misma entidad");
        assertEquals(2, ruta.getParadas().get(0).getItems().size(), "La parada debe contener ambos ítems de entrega");
    }

    @Test
    @DisplayName("agregarEntrega debe crear paradas distintas si las entidades tienen diferente idEntidadBeneficiaria")
    void agregarEntregaCreaParadasDistintasParaEntidadesDiferentes() {
        Camion camion = new Camion("ABC123", 1000.0, 50.0, 5000.0, true);
        Ruta ruta = new Ruta(camion);

        Entidad entidad1 = new Entidad(UUID.randomUUID(), new Direccion());
        Entidad entidad2 = new Entidad(UUID.randomUUID(), new Direccion());

        ItemEntrega item1 = new ItemEntrega(UUID.randomUUID(), 10, UnidadDeMedida.KILOGRAMOS, entidad1);
        ItemEntrega item2 = new ItemEntrega(UUID.randomUUID(), 20, UnidadDeMedida.KILOGRAMOS, entidad2);

        ruta.agregarEntrega(item1);
        ruta.agregarEntrega(item2);

        assertEquals(2, ruta.getParadas().size(), "Deben existir 2 paradas distintas para entidades con diferente ID");
    }
}
