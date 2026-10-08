package ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato "barato" del punto 27 del backlog: no hay un common-lib compartido con logística
 * (está desconectado del build), así que estos DTOs se mantienen a mano en los dos servicios con
 * los mismos nombres de campo por convención. Este test no lee el módulo de logística, pero fija
 * la forma exacta de lo que donaciones-service efectivamente publica, para que un rename o un
 * campo borrado de este lado falle acá en vez de en el primer "Failed to convert message".
 */
class EntregaDTOContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void elMensajePublicadoTieneLaFormaQueLogisticaEspera() throws Exception {
    DireccionDTO direccion = new DireccionDTO();
    direccion.setCalleUno("Av. Siempre Viva");
    direccion.setAltura(742);
    direccion.setCiudad("Springfield");
    direccion.setProvincia("Buenos Aires");
    direccion.setPais("Argentina");

    EntregaDTO entrega = new EntregaDTO(
        List.of(UUID.randomUUID()),
        List.of(new BienDTO(10, "KILOGRAMOS")),
        direccion
    );

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(entrega));

    JsonNode donacionResumen = json.get("donacionResumen");
    assertTrue(donacionResumen.get("idsDonaciones").isArray());
    assertFalse(donacionResumen.get("idsDonaciones").isEmpty());

    JsonNode bien = donacionResumen.get("bienes").get(0);
    assertFalse(bien.get("cantidad").isNull());
    assertFalse(bien.get("unidadDeMedida").isNull());

    JsonNode direccionJson = json.get("entidadBeneficiaria");
    assertFalse(direccionJson.get("calleUno").isNull());
    assertFalse(direccionJson.get("ciudad").isNull());
    assertFalse(direccionJson.get("provincia").isNull());
    assertFalse(direccionJson.get("pais").isNull());
  }
}
