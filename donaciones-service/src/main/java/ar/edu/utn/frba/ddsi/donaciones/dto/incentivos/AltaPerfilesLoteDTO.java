package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Alta de perfiles en lote (POST /api/perfiles/lote de incentivos). Máximo 500 por llamada. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AltaPerfilesLoteDTO {
  private List<IDDTO> perfiles;
}
