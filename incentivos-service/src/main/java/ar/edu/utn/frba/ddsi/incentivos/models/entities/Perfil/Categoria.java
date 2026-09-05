package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Getter
@Setter
@Entity
@NoArgsConstructor // Requerido por JPA/Hibernate para instanciar la clase desde la BD
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idCategoria; // id interno

    private UUID idAdmin;
    private String nombre;
    private Integer posicionSecuencia;

    // mappedBy define quién es el dueño de la relación (la entidad intermedia).
    // orphanRemoval = true borra los registros de la tabla intermedia si los quitas de esta lista.
    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CategoriaMision> categoriaMisiones;

    public Categoria(String nombre,
                     UUID idAdmin,
                     Integer posicionSecuencia,
                     List<Mision> misiones) {
        this.idAdmin = idAdmin;
        this.nombre = nombre;
        this.posicionSecuencia = posicionSecuencia;
        this.categoriaMisiones = new ArrayList<>();

        if (misiones != null) {
            IntStream.range(0, misiones.size()).forEach(i ->
                                                            this.categoriaMisiones.add(
                                                                new CategoriaMision(this, misiones.get(i), i + 1)
                                                            )
            );
        }
    }

    public void agregarMision(Mision mision) {
        this.categoriaMisiones.add(
            new CategoriaMision(this, mision, categoriaMisiones.size() + 1)
        );
    }

    public void eliminarMision(Mision mision) {

        this.categoriaMisiones.removeIf(cm -> cm.getMision().equals(mision));
    }

    public boolean esUltimaMision(Mision mision) {
        List<CategoriaMision> relaciones = relacionesOrdenadas();
        if (relaciones.isEmpty() || mision == null) return false;

        return mision.equals(relaciones.getLast().getMision());
    }

    public Mision primeraMision() {
        List<CategoriaMision> relaciones = relacionesOrdenadas();
        if (relaciones.isEmpty()) return null;

        return relaciones.getFirst().getMision();
    }

    public Mision siguienteMision(Mision misionActual) {
        List<CategoriaMision> relaciones = relacionesOrdenadas();
        if (relaciones.isEmpty() || misionActual == null) return null;

        for (int i = 0; i < relaciones.size(); i++) {
            CategoriaMision categoriaMision = relaciones.get(i);

            if (misionActual.equals(categoriaMision.getMision()) && i + 1 < relaciones.size()) {
                return relaciones.get(i + 1).getMision();
            }
        }

        return null;
    }

    private List<CategoriaMision> relacionesOrdenadas() {
        if (categoriaMisiones == null) return List.of();

        return categoriaMisiones.stream()
                                .sorted(Comparator.comparing(CategoriaMision::getPosicion))
                                .toList();
    }
}