package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Perfil {

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

    public Perfil(UUID idUsuario, String nombreUsuario, String role) {
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
        Insignia insignia = progresoMisionActual.progresarMision(donacion, posicionRanking);
        if (insignia != null) {
            this.insignias.add(insignia);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


}
