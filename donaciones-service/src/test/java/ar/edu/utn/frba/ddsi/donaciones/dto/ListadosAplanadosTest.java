package ar.edu.utn.frba.ddsi.donaciones.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.matchmaking.PropuestaAsignacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 81: los listados ya no serializan el grafo completo")
class ListadosAplanadosTest {

  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  @Test
  @DisplayName("EntidadBeneficiariaDTO no expone necesidades")
  void entidadNoExponeNecesidades() throws Exception {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria();
    entidad.setId(UUID.randomUUID());

    String json = mapper.writeValueAsString(EntidadBeneficiariaDTO.from(entidad));

    assertFalse(json.contains("necesidades"));
  }

  @Test
  @DisplayName("NecesidadDTO no expone donaciones")
  void necesidadNoExponeDonaciones() throws Exception {
    Necesidad necesidad = necesidad();
    necesidad.setId(UUID.randomUUID());

    String json = mapper.writeValueAsString(NecesidadDTO.from(necesidad));

    assertFalse(json.contains("donaciones"));
  }

  @Test
  @DisplayName("DonacionResumenDTO no expone bienes")
  void donacionResumenNoExponeBienes() throws Exception {
    Donacion donacion = new Donacion();
    donacion.setId(UUID.randomUUID());
    donacion.setBienes(new ArrayList<>());

    String json = mapper.writeValueAsString(DonacionResumenDTO.from(donacion));

    assertFalse(json.contains("bienes"));
  }

  @Test
  @DisplayName("PropuestaAsignacionDTO usa resúmenes de entidad y necesidad")
  void propuestaUsaResumenes() throws Exception {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria();
    entidad.setId(UUID.randomUUID());
    PropuestaAsignacion propuesta = new PropuestaAsignacion(entidad, necesidad());

    String json = mapper.writeValueAsString(PropuestaAsignacionDTO.from(propuesta));

    assertFalse(json.contains("necesidades"));
    assertFalse(json.contains("donaciones"));
  }

  private Necesidad necesidad() {
    CategoriaBien categoria = new CategoriaBien("Indumentaria");
    SubcategoriaBien subcategoria = new SubcategoriaBien("Ropa", categoria);
    return new NecesidadExtraordinaria(subcategoria, "Ropa de abrigo", 10);
  }
}
