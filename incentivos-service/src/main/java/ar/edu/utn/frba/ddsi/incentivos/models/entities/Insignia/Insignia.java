package ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * La medalla que se otorga al completar una misión. No tiene setters: se modifica con
 * {@link #actualizar} cuando el admin edita la misión.
 */
@Getter
@Entity
@NoArgsConstructor
public class Insignia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idInsignia;

    private String nombre;

    /** El texto propio de la insignia, distinto del nombre y la descripción de la misión. */
    private String descripcion;

    /** El enlace a la imagen de la insignia, no los bytes. */
    private String urlImagen;

    /** Atajo para cuando solo se tienen nombre y texto, sin imagen. */
    public Insignia(String nombreInsignia, String descripcion) {
        this(nombreInsignia, descripcion, null);
    }

    public Insignia(String nombreInsignia, String descripcion, String urlImagen) {
        this.nombre = nombreInsignia;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
    }

    /**
     * Actualiza los datos de la insignia que ya existe, para no dejar huérfanas las insignias
     * obtenidas. Los campos en null se ignoran.
     */
    public void actualizar(String nombreNuevo, String descripcionNueva, String imagenNueva) {
        if (nombreNuevo != null) {
            this.nombre = nombreNuevo;
        }
        if (descripcionNueva != null) {
            this.descripcion = descripcionNueva;
        }
        if (imagenNueva != null) {
            this.urlImagen = imagenNueva;
        }
    }
}
