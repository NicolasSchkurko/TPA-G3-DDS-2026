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


//dto de ids, se lo envía a servicio de incentivos
public class IDDTO {
  private UUID idUsuario;
  private String nombreUsuario;
}
