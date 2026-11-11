package ar.edu.utn.frba.ddsi.donaciones.matchmaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.SubAtendidos;
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

@DisplayName("SubAtendidos")
class SubAtendidosTest {

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

      List<PropuestaAsignacion> propuestas = new SubAtendidos()
          .rankear(donacion, List.of(entidadInactiva));

      assertTrue(propuestas.isEmpty());
    }
  }

  @Nested
  @DisplayName("Conteo de donaciones")
  class Conteo {

    @Test
    @DisplayName("el score sale del mapa precomputado y no llama a verDonaciones()")
    void usaConteoPrecomputado() {
      UUID entidadId = UUID.randomUUID();
      EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
      when(entidad.getId()).thenReturn(entidadId);

      Necesidad necesidad = mock(Necesidad.class);
      when(necesidad.getId()).thenReturn(UUID.randomUUID());
      when(necesidad.esCompatibleCon(any(Donacion.class))).thenReturn(true);
      when(entidad.getNecesidades()).thenReturn(List.of(necesidad));

      SubAtendidos algoritmo = new SubAtendidos(Map.of(entidadId, 7));
      List<PropuestaAsignacion> ranking = algoritmo.rankear(new Donacion(), List.of(entidad));

      assertEquals(1, ranking.size());
      assertEquals(7.0, ranking.get(0).getScore());
      verify(entidad, never()).verDonaciones();
    }
  }

  @Nested
  @DisplayName("Cantidades precomputadas")
  class Cantidades {

    @Test
    @DisplayName("usa la compatibilidad con la cantidad precomputada")
    void usaCantidadPrecomputada() {
      UUID entidadId = UUID.randomUUID();
      UUID necesidadId = UUID.randomUUID();
      Donacion donacion = mock(Donacion.class);

      Necesidad necesidad = mock(Necesidad.class);
      when(necesidad.getId()).thenReturn(necesidadId);
      when(necesidad.esCompatibleCon(donacion, 3)).thenReturn(true);

      EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
      when(entidad.getId()).thenReturn(entidadId);
      when(entidad.getNecesidades()).thenReturn(List.of(necesidad));

      List<PropuestaAsignacion> ranking = new SubAtendidos(Map.of(entidadId, 0)).rankear(
          donacion, List.of(entidad), Map.of(necesidadId, 3));

      assertEquals(1, ranking.size());
      verify(necesidad).esCompatibleCon(donacion, 3);
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
