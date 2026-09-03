package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Perfil {
    private UUID idUsuario; // id en donaciones
    private UUID idPerfil; // id interno
    private String nombreUsuario;
    private Categoria categoriaActual;
    private List<Insignia> insignias;
    private ProgresoMision progresoMisionActual;
    private PosicionRanking posicionRanking;

    public Perfil(UUID idUsuario, String nombreUsuario, String role) {
        this.idUsuario = idUsuario;
        this.idPerfil = UUID.randomUUID();
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

    public boolean progresarMision(ImpactoDonacion donacion){
        Insignia insignia =  progresoMisionActual.progresarMision(donacion, posicionRanking);
        if (insignia != null) {
            this.insignias.add(insignia);
            return true;
        }
        return false;
    }


}
