package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
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


    //El calculo del ranking se puede hacer con un query que pegue directo a la bdd de mysql. calcularlo siempre
    //trae problemas

    private UUID idUsuario; // id en donaciones

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idPerfil; // id interno

    private String nombreUsuario;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoriaActual;

    @ManyToMany
    @JoinTable(
        name = "perfil_insignia",
        joinColumns = @JoinColumn(name = "perfil_id"),
        inverseJoinColumns = @JoinColumn(name = "insignia_id")
    )
    private List<Insignia> insignias;

    @OneToOne(cascade = CascadeType.ALL)
    private ProgresoMision progresoMisionActual;

    @Embedded
    private PosicionRanking posicionRanking;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = null; //inicializar en perfilService
        this.insignias = new ArrayList<>();
        this.posicionRanking = new PosicionRanking(null);
        this.progresoMisionActual = null; //inicializar en perfilService
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
            this.insignias.add(insignia);

            // Registramos los eventos de dominio para que Spring Data los publique al guardar
            registerEvent(new MisionCompletada(donacion, this));

            if (this.categoriaActual != null && this.categoriaActual.esUltimaMision(misionAnterior)) {
                registerEvent(new UltimaMisionCategoria(this));
            } else {
                registerEvent(new MisionCompletada(null, this));
            }

            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}