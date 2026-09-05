package ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil;

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
import java.util.List;
import java.util.UUID;

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


    @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("posicion ASC")
    private List<CategoriaMision> categoriaMisiones = new ArrayList<>();

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

    public void agregarMision(Mision mision) {
        if (mision == null) return;
        int nuevaPosicion = this.categoriaMisiones.size() + 1;
        this.categoriaMisiones.add(new CategoriaMision(this, mision, nuevaPosicion));
    }

    public void eliminarMision(Mision mision) {
        if (mision == null || this.categoriaMisiones.isEmpty()) return;

        boolean removida = this.categoriaMisiones.removeIf(cm -> cm.getMision().equals(mision));

        if (removida) {
            for (int i = 0; i < this.categoriaMisiones.size(); i++) {
                this.categoriaMisiones.get(i).setPosicion(i + 1);
            }
        }
    }

    public boolean esUltimaMision(Mision mision) {
        if (this.categoriaMisiones.isEmpty() || mision == null) return false;

        return this.categoriaMisiones.stream()
                                     .anyMatch(cm -> cm.getMision().equals(mision) && cm.getPosicion() == this.categoriaMisiones.size());
    }

    public Mision primeraMision() {
        if (this.categoriaMisiones.isEmpty()) return null;

        return this.categoriaMisiones.getFirst().getMision();
    }

    public Mision siguienteMision(Mision misionActual) {
        if (this.categoriaMisiones.isEmpty() || misionActual == null) return null;

        Integer posicionActual = this.categoriaMisiones.stream()
                                                       .filter(cm -> cm.getMision().equals(misionActual))
                                                       .map(CategoriaMision::getPosicion)
                                                       .findFirst()
                                                       .orElse(null);

        if (posicionActual == null) return null;

        return this.categoriaMisiones.stream()
                                     .filter(cm -> cm.getPosicion() == posicionActual + 1)
                                     .map(CategoriaMision::getMision)
                                     .findFirst()
                                     .orElse(null);
    }
}