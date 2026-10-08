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
 * Una etapa del programa de fidelización, con las misiones que el donante cumple en orden.
 *
 * <p>No tiene setters: la secuencia se modifica con {@link #agregarMision} y
 * {@link #eliminarMision}, que mantienen las posiciones correlativas.
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
     * Control de concurrencia optimista: evita que dos ediciones simultáneas dejen la
     * secuencia de posiciones con huecos o repetida.
     */
    @Version
    private Long version;

    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    private List<CategoriaMision> categoriaMisiones = new ArrayList<>();

    /** Una categoría con su secuencia de misiones ya montada. */
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
     * Agrega una misión al final de la secuencia. Es {@code final} porque el constructor la
     * llama.
     */
    public final void agregarMision(Mision mision) {
        if (mision == null) {
            return;
        }
        this.categoriaMisiones.add(new CategoriaMision(this, mision, this.categoriaMisiones.size() + 1));
    }

    /** Saca una misión y renumerar el resto: las posiciones tienen que seguir siendo 1..N. */
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
     * Cambia la posición de la categoría. No renumera: eso lo hace
     * {@code SecuenciaCategoria}.
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
     * La misión que sigue a la dada, o {@code null} si era la última: quien llama usa ese
     * null para pasar a la categoría siguiente.
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
     * Reemplaza nombre y secuencia por los de otra categoría. Las posiciones se recalculan
     * desde el orden en que vienen.
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
            // Se reconstruye en vez de addAll: las CategoriaMision vienen apuntando a la
            // categoría original y al borrarla se llevarían estas filas.
            this.categoriaMisiones.add(new CategoriaMision(this, cm.getMision(),
                    this.categoriaMisiones.size() + 1));
        }
    }
}
