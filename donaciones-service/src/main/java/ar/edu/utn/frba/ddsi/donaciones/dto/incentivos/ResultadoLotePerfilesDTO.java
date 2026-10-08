package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resultado del alta en lote: un motivo por perfil que no se pudo crear. */
@Getter
@Setter
@NoArgsConstructor
public class ResultadoLotePerfilesDTO {
  private int creados;
  private int yaExistian;
  private List<String> errores;
}
