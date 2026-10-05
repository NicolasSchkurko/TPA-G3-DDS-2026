package ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * La medalla que se otorga al completar una misión.
 *
 * <p>No tiene setters: se construye con la misión y se modifica con
 * {@link #actualizar}, que es lo que hace {@code Mision.actualizar} cuando el admin edita
 * la misión. Con setters abiertos, la insignia objetivo podía quedar con datos que no
 * venían de ninguna misión (punto 15).
 */
@Getter
@Entity
@NoArgsConstructor
public class Insignia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idInsignia;

    private String nombre;
    private String descripcion;
    private String urlImagen;

    public Insignia(String nombreInsignia, String descripcion) {
        this.nombre = nombreInsignia;
        this.descripcion = descripcion;
        this.urlImagen = null;
    }

    /**
     * Actualiza nombre y descripción de la insignia objetivo de una misión.
     *
     * <p>Se edita la insignia <b>que ya existe</b> y no se reemplaza por la que trae el
     * DTO: las filas de {@code InsigniaObtenida} apuntan a ella, así que reemplazarla
     * dejaría huérfanas todas las insignias que los donantes ya obtuvieron.
     *
     * <p>Un nombre en null se ignora, y una descripción en null también: el admin puede
     * editar solo el nombre sin borrar la descripción que ya estaba.
     */
    public void actualizar(String nombreNuevo, String descripcionNueva) {
        if (nombreNuevo != null) {
            this.nombre = nombreNuevo;
        }
        if (descripcionNueva != null) {
            this.descripcion = descripcionNueva;
        }
    }
}
