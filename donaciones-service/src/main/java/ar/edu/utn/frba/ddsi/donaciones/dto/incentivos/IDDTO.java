package ar.edu.utn.frba.ddsi.donaciones.dto.incentivos;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// Payload del alta de perfil en incentivos (POST /api/perfiles).
public class IDDTO {
  private UUID idUsuario;
  private String nombreUsuario;
  /** El perfil nace siempre como DONANTE; incentivos lo usa para su público/roles. */
  private String role;
}