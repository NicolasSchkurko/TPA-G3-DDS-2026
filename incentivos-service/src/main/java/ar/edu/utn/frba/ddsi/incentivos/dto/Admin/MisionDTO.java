package ar.edu.utn.frba.ddsi.incentivos.dto.Admin;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
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

    /**
     * El texto propio de la insignia objetivo (punto 24).
     *
     * <p>Es opcional a propósito, y no por pereza: el enunciado pide que la insignia tenga
     * nombre, descripción e imagen, pero obligar a los tres en el alta rompe a cualquier
     * cliente que hoy solo mande el nombre. Con esto los tres se pueden cargar, y los que no
     * se manden quedan en null.
     *
     * <p>Ojo con la asimetría: {@code descripcion} es el texto de la <b>misión</b> y este es
     * el de la <b>insignia</b>. Antes el de la insignia se rellenaba solo con el nombre de la
     * misión, así que el campo directamente no existía y lo que se guardaba era el texto que
     * no correspondía.
     */
    private String insigniaDescripcion;

    /**
     * Dónde se puede ver la imagen de la insignia objetivo.
     *
     * <p>Una URL, no la imagen: el campo lleva el enlace y no los bytes. Ver el javadoc de
     * {@code Insignia.urlImagen} para por qué.
     */
    private String insigniaUrlImagen;

    @NotNull(message = "La misión requiere una regla de progreso")
    @Valid
    private ReglaDTO regla;

    public MisionDTO(String nomM, String descripcion, String nomI,
                     ConstanciaDTO cia,
                     String atributo,
                     OperacionDTO op) {
        this(nomM, descripcion, nomI, null, null, cia, atributo, op);
    }

    public MisionDTO(String nomM, String descripcion, String nomI,
                     String descripcionInsignia, String urlImagenInsignia,
                     ConstanciaDTO cia,
                     String atributo,
                     OperacionDTO op) {
        this.nombreMision = nomM;
        this.descripcion = descripcion;
        this.insigniaObjetivo = nomI;
        this.insigniaDescripcion = descripcionInsignia;
        this.insigniaUrlImagen = urlImagenInsignia;
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

        Insignia insignia = mision.getInsigniaObjetivo();

        return new MisionDTO(
            mision.getNombreMision(),
            mision.getDescripcion(),
            insignia == null ? null : insignia.getNombre(),
            insignia == null ? null : insignia.getDescripcion(),
            insignia == null ? null : insignia.getUrlImagen(),
            constancia,
            atributo == null ? null : atributo.name(),
            OperacionDTO.desdeEntidad(operacion)
        );
    }
}
