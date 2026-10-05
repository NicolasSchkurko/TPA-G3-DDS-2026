package ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Una etapa del programa de fidelización, con las misiones que el donante tiene que
 * cumplir para recorrerlas en orden.
 *
 * <p>No tiene setters. La secuencia de misiones se modifica con
 * {@link #agregarMision} y {@link #eliminarMision}, que la mantienen con posiciones
 * correlativas, y la posición en el programa con {@link #moverAPosicion}. Con setters
 * abiertos era posible dejar una categoría con dos misiones en la misma posición, y
 * entonces {@link #siguienteMision} devuelve null para un donante que aún tiene
 * misiones por hacer: queda trabado sin nada que completar.
 */
@Getter
@Entity
@NoArgsConstructor // Requerido por JPA/Hibernate para instanciar la clase desde la BD
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idCategoria; // id interno

    private UUID idAdmin;
    private String nombre;
    private Integer posicionSecuencia;

    /**
     * Control de concurrencia optimista (punto 36).
     *
     * <p>Hace falta por el mismo motivo que en {@code Perfil}:{@code actualizarCategoria}
     * lee la categoría, arma una nueva y la guarda. Dos admins editando la misma categoría
     * al mismo tiempo, o el scheduler de la secuencia corriendo mientras un admin edita,
     * hacen que la segunda escritura pise a la primera y la secuencia de posiciones quede
     * con huecos o con dos categorías en el mismo lugar.
     *
     * <p>La estructura de posiciones se garantiza en la aplicación y no con un
     * {@code unique} en la base (punto 19), justamente porque el movimiento es un
     * {@code UPDATE} en bloque de varias filas. Eso deja la concurrencia en manos del
     * {@code @Version}.
     */
    @Version
    private Long version;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    private List<CategoriaMision> categoriaMisiones = new ArrayList<>();

    /**
     * Una categoría con su secuencia de misiones ya montada.
     *
     * <p>El aviso de {@code this-escape} está suprimido a propósito: al agregar cada
     * misión hay que pasarle la categoría a {@code CategoriaMision}, y eso inevitablemente
     * entrega {@code this} antes de terminar de construir. Es seguro porque
     * {@code CategoriaMision} solo guarda la referencia, no la usa; y no hay forma de
     * es justamente lo que se sacó (punto 23).
     */
    @SuppressWarnings("this-escape")
    public Categoria(String nombre,
                     UUID idAdmin,
                     Integer posicionSecuencia,
                     List<Mision> misiones) {
        this.idAdmin = idAdmin;
        this.nombre = nombre;
        this.posicionSecuencia = posicionSecuencia;

        if (misiones != null) {
            for (Mision mision : misiones) {
                this.agregarMision(mision);
            }
        }
    }

    /**
     * Agrega una misión al final de la secuencia. La posición se calcula sola, así que no
     * hay forma de meter una misión con la posición equivocada.
     *
     * <p>Es {@code final} porque el constructor la llama: si una subclase la sobrescribiera,
     * vería un objeto a medio construir. Hibernate no la necesita sobrescribir (sus proxies
     * delegan los getters), así que no se pierde nada.
     */
    public final void agregarMision(Mision mision) {
        if (mision == null) {
            return;
        }
        this.categoriaMisiones.add(new CategoriaMision(this, mision, this.categoriaMisiones.size() + 1));
    }

    /**
     * Saca una misión y cierra el hueco que deja, renumerando el resto.
     *
     * <p>Renumerar es indispensable: las posiciones tienen que seguir siendo 1..N, porque
     * {@link #siguienteMision} busca "la que está en la posición siguiente" y un hueco las
     * deja de encontrar.
     */
    public void eliminarMision(Mision mision) {
        if (mision == null) {
            return;
        }

        boolean removida = this.categoriaMisiones.removeIf(cm -> cm.getMision().equals(mision));
        if (!removida) {
            return;
        }

        for (int i = 0; i < this.categoriaMisiones.size(); i++) {
            this.categoriaMisiones.get(i).moverAPosicion(i + 1);
        }
    }

    /**
     * Cambia la posición de la categoría en el programa.
     *
     * <p>No renumera nada: eso es trabajo de {@code SecuenciaCategoria}, que mueve las
     * demás con UPDATE en bloque. Acá solo se deja la posición nueva en la entidad.
     */
    public void moverAPosicion(Integer posicion) {
        this.posicionSecuencia = posicion;
    }

    /** La primera misión de la secuencia, o {@code null} si la categoría no tiene. */
    public Mision primeraMision() {
        if (this.categoriaMisiones.isEmpty()) {
            return null;
        }
        return this.categoriaMisiones.getFirst().getMision();
    }

    /**
     * La misión que sigue a la dada, o {@code null} si era la última.
     *
     * <p>Devolver {@code null} para la última es lo que hace de "última de la categoría":
     * quien llama ({@code PerfilService.asignarSiguienteMision}) usa ese null para saber
     * que tiene que pasar a la categoría siguiente.
     */
    public Mision siguienteMision(Mision misionActual) {
        if (this.categoriaMisiones.isEmpty() || misionActual == null) {
            return null;
        }

        Integer posicionActual = this.categoriaMisiones.stream()
                                                       .filter(cm -> cm.getMision().equals(misionActual))
                                                       .map(CategoriaMision::getPosicion)
                                                       .findFirst()
                                                       .orElse(null);

        if (posicionActual == null) {
            return null;
        }

        return this.categoriaMisiones.stream()
                                     .filter(cm -> cm.getPosicion() == posicionActual + 1)
                                     .map(CategoriaMision::getMision)
                                     .findFirst()
                                     .orElse(null);
    }

    /**
     * Reemplaza nombre y secuencia por los de otra categoría.
     *
     * <p>Se llama al editar una categoría. Las posiciones de las misiones se recalculan
     * desde el orden en que vienen, que es el que el admin pidió, así que editando la
     * secuencia se pueden reordenar las misiones.
     */
    public void actualizarCon(Categoria cambios) {
        if (cambios.getNombre() != null) {
            this.nombre = cambios.getNombre();
        }

        List<CategoriaMision> nuevaSecuencia = cambios.getCategoriaMisiones();
        if (nuevaSecuencia == null || nuevaSecuencia.isEmpty()) {
            return;
        }

        this.categoriaMisiones.clear();

        for (CategoriaMision cm : nuevaSecuencia) {
            // Se reconstruye en vez de addAll: las CategoriaMision que vienen de la otra
            // categoría seguirían apuntando a ella, y al borrar la categoría original se
            // llevarían estas filas.
            this.categoriaMisiones.add(new CategoriaMision(this, cm.getMision(),
                    this.categoriaMisiones.size() + 1));
        }
    }
}
