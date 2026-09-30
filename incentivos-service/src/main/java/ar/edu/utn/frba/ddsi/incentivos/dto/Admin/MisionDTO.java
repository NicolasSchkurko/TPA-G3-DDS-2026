package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MisionDTO {
    private String nombreMision;
    private String descripcion;
    private String insigniaObjetivo;
    private ReglaDTO regla;

    public MisionDTO(String nomM, String descripcion, String nomI,
                     ConstanciaDTO cia,
                     String atributo,
                     OperacionDTO op) {
        this.nombreMision = nomM;
        this.descripcion = descripcion;
        this.insigniaObjetivo = nomI;
        this.regla = new ReglaDTO(cia, atributo, op);
    }

    public static MisionDTO desdeEntidad(Mision mision) {
        if (mision == null) {
            return null;
        }

        ReglaConstancia reglaConstancia = mision.getReglaDeProgreso().getConstancia();
        ConstanciaDTO constancia = reglaConstancia == null
                                   ? null
                                   : new ConstanciaDTO(
            reglaConstancia.getCantidad(),
            reglaConstancia.getUnidadTiempo().toString()
        );

        return new MisionDTO(
            mision.getNombreMision(),
            mision.getDescripcion(),
            mision.getInsigniaObjetivo().getNombre(),
            constancia,
            mision.getReglaDeProgreso().getAtributo().name(),
            OperacionDTO.desdeEntidad(mision.getReglaDeProgreso().getOperacion())
        );
    }
}