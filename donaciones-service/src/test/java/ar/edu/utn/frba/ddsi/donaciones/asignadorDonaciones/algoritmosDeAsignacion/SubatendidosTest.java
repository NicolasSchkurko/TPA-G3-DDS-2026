package ar.edu.utn.frba.ddsi.donaciones.asignadorDonaciones.algoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.SubAtendidos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SubatendidosTest {

  private final SubAtendidos algoritmo = new SubAtendidos();

  @Test
  void testRankearSinEntidadesRetornaListaVacia() {
    Donacion donacion = new Donacion();
    List<PropuestaAsignacion> resultado = algoritmo.rankear(donacion, Collections.emptyList());

    assertNotNull(resultado);
    assertTrue(resultado.isEmpty());
  }

//  @Test
//  void testFiltrarYContarDonacionesDelUltimoTrimestre() {
//    Donacion donacionInyectada = new Donacion();
//
//    Necesidad necesidad = new Necesidad() {
//      @Override public boolean esCompatibleCon(Donacion d) { return true; }
//    };
//
//    Donacion donacionReciente = new Donacion() {
//      @Override public LocalDate getFechaEntrega() { return LocalDate.now().minusMonths(1); }
//    };
//    Donacion donacionVieja = new Donacion() {
//      @Override public LocalDate getFechaEntrega() { return LocalDate.now().minusMonths(5); }
//    };
//    Donacion donacionNoEntregada = new Donacion() {
//      @Override public LocalDate getFechaEntrega() { return null; }
//    };
//
//    EntidadBeneficiaria entidad = new EntidadBeneficiaria() {
//      @Override public List<Necesidad> getNecesidades() { return List.of(necesidad); }
//      @Override public List<Donacion> verDonaciones() {
//        return List.of(donacionReciente, donacionVieja, donacionNoEntregada);
//      }
//    };
//
//    List<PropuestaAsignacion> resultado = algoritmo.rankear(donacionInyectada, List.of(entidad));
//
//    assertEquals(1, resultado.size());
//    assertEquals(1.0, resultado.get(0).getScore(), 0.001);
//  }
//
//  @Test
//  void testComportamientoMaxHeapYFiltroTop10() {
//    Donacion donacionInyectada = new Donacion();
//    List<EntidadBeneficiaria> entidades = new ArrayList<>();
//
//    for (int i = 1; i <= 12; i++) {
//      final int cantidadDonacionesAsignar = i;
//
//      Necesidad nec = new Necesidad() {
//        @Override public boolean esCompatibleCon(Donacion d) { return true; }
//      };
//
//      EntidadBeneficiaria ent = new EntidadBeneficiaria() {
//        @Override public List<Necesidad> getNecesidades() { return List.of(nec); }
//        @Override public List<Donacion> verDonaciones() {
//          List<Donacion> lista = new ArrayList<>();
//          for (int j = 0; j < cantidadDonacionesAsignar; j++) {
//            lista.add(new Donacion() {
//              @Override public LocalDate getFechaEntrega() { return LocalDate.now(); }
//            });
//          }
//          return lista;
//        }
//      };
//      entidades.add(ent);
//    }
//
//    List<PropuestaAsignacion> resultado = algoritmo.rankear(donacionInyectada, entidades);
//
//    assertEquals(10, resultado.size());
//    assertEquals(1.0, resultado.get(0).getScore(), 0.001);
//    assertEquals(10.0, resultado.get(9).getScore(), 0.001);
//
//    for (int i = 0; i < resultado.size() - 1; i++) {
//      assertTrue(resultado.get(i).getScore() <= resultado.get(i+1).getScore());
//    }
// }
}