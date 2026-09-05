package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCompletada;
import ar.edu.utn.frba.ddsi.incentivos.models.events.UltimaMisionCategoria;
import org.springframework.data.domain.AbstractAggregateRoot;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Perfil extends AbstractAggregateRoot<Perfil> {

    private UUID idUsuario; // id en donaciones

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idPerfil; // id interno

    private String nombreUsuario;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoriaActual;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InsigniaObtenida> insigniasObtenidas;

    @OneToOne(cascade = CascadeType.ALL)
    private ProgresoMision progresoMisionActual;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = null;
        this.insigniasObtenidas = new ArrayList<>();
        this.progresoMisionActual = null;
    }

    public void verificarProgresoMision(){
        if (progresoMisionActual != null)
            progresoMisionActual.evaluarConstancia();
    }

    public Boolean progresarMision(ImpactoDonacion donacion){
        if (progresoMisionActual == null) return false;

        Mision misionAnterior = progresoMisionActual.getMision();
        Insignia insignia = progresoMisionActual.progresarMision(donacion);

        if (insignia != null) {
            this.insigniasObtenidas.add(new InsigniaObtenida(this, insignia));


            registerEvent(new MisionCompletada(donacion, this));

            if (this.categoriaActual != null && this.categoriaActual.esUltimaMision(misionAnterior)) {
                registerEvent(new UltimaMisionCategoria(this));
            }

            return true;
        }
        return false;
    }
}