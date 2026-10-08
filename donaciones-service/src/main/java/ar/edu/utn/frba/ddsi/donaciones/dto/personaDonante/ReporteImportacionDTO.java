package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class ReporteImportacionDTO {
  private final UUID id;
  private final String estado;
  private final int totalFilas;
  private final int importadosExitosos;
  private final int fallidos;
  private final List<String> errores;

  private ReporteImportacionDTO(UUID id, String estado, int totalFilas, int importadosExitosos,
                                 int fallidos, List<String> errores) {
    this.id = id;
    this.estado = estado;
    this.totalFilas = totalFilas;
    this.importadosExitosos = importadosExitosos;
    this.fallidos = fallidos;
    this.errores = errores;
  }

  public static ReporteImportacionDTO enProgreso(UUID id) {
    return new ReporteImportacionDTO(id, "EN_PROGRESO", 0, 0, 0, List.of());
  }

  // errores puede incluir tanto filas de CSV mal formadas como donantes que fallaron al crearse;
  // se acota a los primeros N para no devolver un reporte gigante en archivos muy rotos.
  public static ReporteImportacionDTO completado(UUID id, int totalFilas, int importadosExitosos, List<String> errores) {
    List<String> erroresAcotados = errores.size() > 50 ? errores.subList(0, 50) : errores;
    return new ReporteImportacionDTO(id, "COMPLETADO", totalFilas, importadosExitosos, errores.size(), erroresAcotados);
  }

  public static ReporteImportacionDTO fallido(UUID id, String error) {
    return new ReporteImportacionDTO(id, "FALLIDO", 0, 0, 1, List.of(error));
  }
}
