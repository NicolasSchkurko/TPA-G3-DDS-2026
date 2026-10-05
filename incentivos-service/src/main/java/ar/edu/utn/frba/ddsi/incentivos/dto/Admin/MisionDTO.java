package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MisionDTO {

    @NotBlank(message = "La misión requiere un nombre")
    private String nombreMision;

    private String descripcion;

    @NotBlank(message = "La misión requiere una insignia objetivo")
    private String insigniaObjetivo;

    @NotNull(message = "La misión requiere una regla de progreso")
    @Valid
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

    /**
 * Proyecta una misión a DTO. Es null-safe en toda la cadena: una sola misión con la
 * regla incompleta no puede romper el listado completo de misiones, que es lo que
 * pasaba antes con un {@code NullPointerException} a mitad del {@code map}.
 */
    public static MisionDTO desdeEntidad(Mision mision) {
        if (mision == null) {
            return null;
        }

        Regla regla = mision.getReglaDeProgreso();
        Operacion operacion = regla == null ? null : regla.getOperacion();
        ReglaConstancia reglaConstancia = regla == null ? null : regla.getConstancia();
        AtributoImpacto atributo = regla == null ? null : regla.getAtributo();

        ConstanciaDTO constancia = reglaConstancia == null
                                   ? null
                                   : new ConstanciaDTO(
            reglaConstancia.getCantidad(),
            // name() y no toString(): ChronoUnit.toString() devuelve "Months" en
            // camelCase y el resto de la API usa mayúsculas ("COINCIDENCIAS",
            // "CANTIDAD_BIENES"). Al leerlo, MisionFactory normaliza las dos formas.
            reglaConstancia.getUnidadTiempo() == null
                ? null
                : reglaConstancia.getUnidadTiempo().name()
        );

        String nombreInsignia = mision.getInsigniaObjetivo() == null
                               ? null
                               : mision.getInsigniaObjetivo().getNombre();

        return new MisionDTO(
            mision.getNombreMision(),
            mision.getDescripcion(),
            nombreInsignia,
            constancia,
            atributo == null ? null : atributo.name(),
            OperacionDTO.desdeEntidad(operacion)
        );
    }
}