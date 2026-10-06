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

    /**
     * El texto propio de la insignia, que el enunciado pide como segundo campo.
     *
     * <p>Antes no existía como dato: el constructor de {@link Mision} pasaba el nombre de la
     * misión, así que el texto de la insignia era en realidad el nombre de la misión (punto
     * 24). Al editar, en cambio, venía la descripción de la insignia entrante. O sea que el
     * texto se derivaba de dos fuentes distintas según si la misión se creaba o se editaba, y
     * ninguna de las dos era el texto de la insignia.
     */
    private String descripcion;

    /**
     * Dónde se puede ver la imagen de la insignia.
     *
     * <p><b>Es una referencia y no la imagen.</b> El enunciado pide que la insignia
     * especifique imagen, y lo que este campo guarda es el <em>enlace</em> a ella, del
     * mismo modo que {@link #descripcion} guarda el texto y no el lugar donde está escrito.
     * Guardar los bytes sería meter en el modelo una decisión de infraestructura que
     * pertenece al servicio que los publica, y ataría el dominio a un almacén concreto.
     *
     * <p>La columna existía desde el principio pero no había por dónde cargarla: quedaba
     * siempre en {@code null}. Ahora entra por el DTO.
     */
    private String urlImagen;

    /**
 * El atajo para cuando solo se tienen nombre y texto, sin imagen.
     *
     * <p>La imagen acepta null a propósito: el enunciado la pide como tercer campo pero no
     * todos los clientes la mandan, y obligar a cargarla rompería los que hoy solo mandan
     * nombre y descripción.
     */
    public Insignia(String nombreInsignia, String descripcion) {
        this(nombreInsignia, descripcion, null);
    }

    public Insignia(String nombreInsignia, String descripcion, String urlImagen) {
        this.nombre = nombreInsignia;
        this.descripcion = descripcion;
        this.urlImagen = urlImagen;
    }

    /**
     * Actualiza los datos de la insignia objetivo de una misión.
     *
     * <p>Se edita la insignia <b>que ya existe</b> y no se reemplaza por la que trae el
     * DTO: las filas de {@code InsigniaObtenida} apuntan a ella, así que reemplazarla
     * dejaría huérfanas todas las insignias que los donantes ya obtuvieron.
     *
     * <p>Cada campo en null se ignora: el admin puede editar solo el nombre sin borrar la
     * descripción ni la imagen que ya estaban. Es lo que hace que un PUT parcial no borre
     * datos.
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
