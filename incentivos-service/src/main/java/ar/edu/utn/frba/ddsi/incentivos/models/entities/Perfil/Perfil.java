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
    private Mision misionActual;
    private PosicionRanking posicionRanking;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.idPerfil = UUID.randomUUID();
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = null; //inicializar en gestorPerfiles
        this.insignias = new ArrayList<>();
        this.posicionRanking = new PosicionRanking(null);
        this.misionActual = null; //se inicializa en gestorPerfiles
    }

    public void verificarProgresoMision(){
        misionActual.evaluarConstancia();
    }

    public boolean progresarMision(ImpactoDonacion donacion){
        misionActual.evaluarConstancia();
        misionActual.evaluarProgreso(donacion);

        if (misionActual.estaCompleta()) {
            this.otorgarInsignia();
            this.sumarMisionCumplida();
            return true;
        }
        return false;
    }

    private void otorgarInsignia() {
        Insignia insignia = misionActual.getInsigniaObjetivo();
        insignia.setFechaObtencion(LocalDate.now());
        insignias.add(insignia);
    }

    private void sumarMisionCumplida(){
        Integer current = posicionRanking.getMisionesCumplidasEnPeriodo();
        posicionRanking.setMisionesCumplidasEnPeriodo(current + 1);
    }
}
