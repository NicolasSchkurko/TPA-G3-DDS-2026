package ar.edu.utn.frba.ddsi.donaciones.matchmaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.CompatibilidadSemantica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienPerecedero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompatibilidadSemantica")
class CompatibilidadSemanticaTest {

  @Nested
  @DisplayName("Soft delete")
  class SoftDelete {

    @Test
    @DisplayName("ignora entidades inactivas")
    void ignoraEntidadesInactivas() {
      Donacion donacion = donacionCompatible();
      EntidadBeneficiaria entidadInactiva = new EntidadBeneficiaria();
      entidadInactiva.agregarNecesidad(new NecesidadExtraordinaria(subcategoria(), "Leche", 50));
      entidadInactiva.darDeBaja();

      List<PropuestaAsignacion> propuestas = new CompatibilidadSemantica()
          .rankear(donacion, List.of(entidadInactiva));

      assertTrue(propuestas.isEmpty());
    }

    @Test
    @DisplayName("ignora necesidades inactivas en entidades activas")
    void ignoraNecesidadesInactivas() {
      Donacion donacion = donacionCompatible();
      EntidadBeneficiaria entidadActiva = new EntidadBeneficiaria();
      Necesidad necesidadCancelada = new NecesidadExtraordinaria(subcategoria(), "Leche", 50);
      necesidadCancelada.setActiva(false);
      entidadActiva.agregarNecesidad(necesidadCancelada);

      List<PropuestaAsignacion> propuestas = new CompatibilidadSemantica()
          .rankear(donacion, List.of(entidadActiva));

      assertTrue(propuestas.isEmpty());
    }
  }

  @Nested
  @DisplayName("Cantidades precomputadas")
  class Cantidades {

    @Test
    @DisplayName("usa esCompatibleCon/cantidadFaltante con la cantidad precomputada")
    void usaCantidadPrecomputada() {
      UUID necesidadId = UUID.randomUUID();
      Donacion donacion = mock(Donacion.class);
      when(donacion.sumaCantidadBienes()).thenReturn(5);

      Necesidad necesidad = mock(Necesidad.class);
      when(necesidad.getId()).thenReturn(necesidadId);
      when(necesidad.esCompatibleCon(donacion, 3)).thenReturn(true);
      when(necesidad.cantidadFaltante(3)).thenReturn(7);

      EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
      when(entidad.isActiva()).thenReturn(true);
      when(entidad.getNecesidades()).thenReturn(List.of(necesidad));

      List<PropuestaAsignacion> ranking = new CompatibilidadSemantica().rankear(
          donacion, List.of(entidad), Map.of(necesidadId, 3));

      assertEquals(1, ranking.size());
      verify(necesidad).esCompatibleCon(donacion, 3);
      verify(necesidad).cantidadFaltante(3);
      verify(necesidad, never()).getDonaciones();
    }
  }

  private static SubcategoriaBien subcategoria() {
    return new SubcategoriaBien("Leche", new CategoriaBien("Alimentos"));
  }

  private static Donacion donacionCompatible() {
    SubcategoriaBien leche = subcategoria();
    Donacion donacion = new Donacion();
    donacion.setDescripcion("Cajas de leche");
    donacion.setSubcategoria(leche);
    donacion.agregarBien(new BienPerecedero(
        "Leche", leche, null, 10, UnidadDeMedida.UNIDADES, LocalDate.now().plusMonths(1)));
    return donacion;
  }
}
