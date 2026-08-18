package ar.edu.utn.frba.ddsi.donaciones.donacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienConEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.BienPerecedero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SegmentadorDonacionesTest {

  private Donante donanteMock;
  private SubcategoriaBien subCategoriaRopaMock;
  private SubcategoriaBien subCategoriaAlimentoMock;

  @BeforeEach
  void setUp() {
    donanteMock = mock(Donante.class);

    subCategoriaRopaMock = mock(SubcategoriaBien.class);
    when(subCategoriaRopaMock.getNombre()).thenReturn("Ropa");

    subCategoriaAlimentoMock = mock(SubcategoriaBien.class);
    when(subCategoriaAlimentoMock.getNombre()).thenReturn("Alimentos");
  }

  @Test
  @DisplayName("Debe agrupar bienes comunes de la misma subcategoría en una sola donación")
  void segmentar_BienesComunes_AgrupaPorSubcategoria() {
    Bien bienRopa1 = mock(Bien.class);
    when(bienRopa1.getSubcategoria()).thenReturn(subCategoriaRopaMock);

    Bien bienRopa2 = mock(Bien.class);
    when(bienRopa2.getSubcategoria()).thenReturn(subCategoriaRopaMock);

    Bien bienAlimento = mock(Bien.class);
    when(bienAlimento.getSubcategoria()).thenReturn(subCategoriaAlimentoMock);

    List<Bien> bienesRecibidos = Arrays.asList(bienRopa1, bienRopa2, bienAlimento);

    List<Donacion> donaciones = SegmentadorDonaciones.segmentar(donanteMock, bienesRecibidos);

    // Debería generar 2 donaciones, una para Ropa y otra para Alimentos
    assertEquals(2, donaciones.size());

    // Tomo la donacion de la subcategoria ropa

    Donacion donacionRopa = donaciones.stream()
                                      .filter(d -> d.getSubcategoria().getNombre().equals("Ropa"))
                                      .findFirst().get();

    assertEquals(2, donacionRopa.getBienes().size());
    assertEquals(donanteMock, donacionRopa.getDonante());
  }

  @Test
  @DisplayName("Debe separar bienes perecederos si tienen distinta fecha de vencimiento")
  void segmentar_BienesPerecederos_AgrupaPorFecha() {
    LocalDate fechaHoy = LocalDate.now();
    LocalDate fechaManana = fechaHoy.plusDays(1);

    BienPerecedero alimentoHoy1 = mock(BienPerecedero.class);
    when(alimentoHoy1.getSubcategoria()).thenReturn(subCategoriaAlimentoMock);
    when(alimentoHoy1.getFechaVencimiento()).thenReturn(fechaHoy);

    BienPerecedero alimentoHoy2 = mock(BienPerecedero.class);
    when(alimentoHoy2.getSubcategoria()).thenReturn(subCategoriaAlimentoMock);
    when(alimentoHoy2.getFechaVencimiento()).thenReturn(fechaHoy);

    BienPerecedero alimentoManana = mock(BienPerecedero.class);
    when(alimentoManana.getSubcategoria()).thenReturn(subCategoriaAlimentoMock);
    when(alimentoManana.getFechaVencimiento()).thenReturn(fechaManana);

    List<Bien> bienesRecibidos = Arrays.asList(alimentoHoy1, alimentoHoy2, alimentoManana);

    List<Donacion> donaciones = SegmentadorDonaciones.segmentar(donanteMock, bienesRecibidos);

    // Debería generar 2 donaciones porque hay 2 fechas distintas, aunque sean la misma subcategoría
    assertEquals(2, donaciones.size());
  }

  @Test
  @DisplayName("Debe separar bienes con estado (ej. Ropa) si uno es nuevo y otro es usado")
  void segmentar_BienesConEstado_AgrupaPorEstado() {
    BienConEstado ropaNueva1 = mock(BienConEstado.class);
    when(ropaNueva1.getSubcategoria()).thenReturn(subCategoriaRopaMock);
    when(ropaNueva1.isUsado()).thenReturn(false);

    BienConEstado ropaNueva2 = mock(BienConEstado.class);
    when(ropaNueva2.getSubcategoria()).thenReturn(subCategoriaRopaMock);
    when(ropaNueva2.isUsado()).thenReturn(false);

    BienConEstado ropaUsada = mock(BienConEstado.class);
    when(ropaUsada.getSubcategoria()).thenReturn(subCategoriaRopaMock);
    when(ropaUsada.isUsado()).thenReturn(true);

    List<Bien> bienesRecibidos = Arrays.asList(ropaNueva1, ropaNueva2, ropaUsada);

    List<Donacion> donaciones = SegmentadorDonaciones.segmentar(donanteMock, bienesRecibidos);

    // Debería generar 2 donaciones: una para Ropa-NUEVO (2 ítems) y otra para Ropa-USADO (1 ítem)
    assertEquals(2, donaciones.size());
  }

  @Test
  @DisplayName("Debe devolver lista vacía si no se envían bienes")
  void segmentar_SinBienes_DevuelveVacio() {
    List<Donacion> donaciones = SegmentadorDonaciones.segmentar(donanteMock, new ArrayList<>());

    assertTrue(donaciones.isEmpty());
  }
}