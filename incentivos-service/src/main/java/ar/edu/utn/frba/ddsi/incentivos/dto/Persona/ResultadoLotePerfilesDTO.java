package ar.edu.utn.frba.ddsi.incentivos.dto.Persona;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Resultado de un alta en lote. Los perfiles que ya existían no son un error: hace idempotente
 * reintentar una importación parcial.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoLotePerfilesDTO {
    private int creados;
    private int yaExistian;

    /** Un motivo por perfil que no se pudo crear; vacío si salió todo bien. */
    private List<String> errores;
}
