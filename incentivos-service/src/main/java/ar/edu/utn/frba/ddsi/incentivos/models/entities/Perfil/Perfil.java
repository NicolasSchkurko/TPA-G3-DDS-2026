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
        Mision misionActual = getMisionActual();
        if (misionActual == null) return false;
        if (progresoMisionActual == null || progresoMisionActual.getMision() != misionActual) {
            progresoMisionActual = new ProgresoMision(misionActual);
        }
        progresoMisionActual.evaluarConstancia();
        progresoMisionActual.evaluarProgreso(donacion);

        if (progresoMisionActual.estaCompleta()) {
            this.otorgarInsignia();
            this.sumarMisionCumplida();
            return true;
        }
        return false;
    }

    private void otorgarInsignia() {
        Insignia insignia = getMisionActual().getInsigniaObjetivo();
        insignia.setFechaObtencion(LocalDate.now());
        insignias.add(insignia);
    }

    private void sumarMisionCumplida(){
        Integer current = posicionRanking.getMisionesCumplidasEnPeriodo();
        posicionRanking.setMisionesCumplidasEnPeriodo(current + 1);
    }

    public void setMisionActual(Mision misionActual) {
        this.progresoMisionActual = misionActual == null ? null : new ProgresoMision(misionActual);
    }

    public Mision getMisionActual() {
        return progresoMisionActual == null ? null : progresoMisionActual.getMision();
    }
}
