package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
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
        this.misionActual = null; //se inicializa en personaService (gestorPerfiles) cuando se crea
    }

    public void verificarProgresoMision(){
        misionActual.evaluarConstancia();
    }

    public void progresarMision(ImpactoDonacion donacion){
        misionActual.evaluarProgreso(donacion);

        if (misionActual.estaCompleta()) {
            this.otorgarInsignia();
            this.sumarMisionCumplida();
        }
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

    public Perfil clonar() {
        Perfil copia = new Perfil(this.idUsuario, this.nombreUsuario);

        copia.setCategoriaActual(this.categoriaActual);
        copia.setInsignias(this.insignias);
        copia.setMisionActual(this.misionActual);

        return copia;
    }
}