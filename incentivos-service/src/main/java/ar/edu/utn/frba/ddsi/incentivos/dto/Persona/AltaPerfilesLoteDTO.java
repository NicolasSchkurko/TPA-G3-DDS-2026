package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Alta de perfiles en lote (la importación CSV de donaciones manda hasta 500 por llamada). */
@Getter
@Setter
@NoArgsConstructor
public class AltaPerfilesLoteDTO {

    @Valid
    @NotEmpty(message = "El lote requiere al menos un perfil")
    @Size(max = 500, message = "El lote no puede traer más de 500 perfiles")
    private List<PerfilDonanteDTO> perfiles;

    public AltaPerfilesLoteDTO(List<PerfilDonanteDTO> perfiles) {
        this.perfiles = perfiles;
    }
}
