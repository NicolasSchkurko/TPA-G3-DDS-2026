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
        this.misionActual = null; //se inicializa en gestorPerfiles
    }

    public void verificarProgresoMision(){
        misionActual.evaluarConstancia();
    }

    public List<Boolean> progresarMision(ImpactoDonacion donacion){
        List<Boolean> resultados = new ArrayList<>();
        misionActual.evaluarConstancia();
        misionActual.evaluarProgreso(donacion);

        if (misionActual.estaCompleta()) {
            resultados.add(Boolean.TRUE); //aviso que se completo la mision
            //x ende se suma insignia y avanza posicion ranking
            this.otorgarInsignia();
            this.sumarMisionCumplida();

            Boolean resultado = this.progresarCategoria();
            resultados.add(resultado);
        }
        resultados.add(Boolean.FALSE);

        return resultados;
    }

    private Boolean progresarCategoria(){
        if (categoriaActual.esUltimaMision(misionActual)){
            return Boolean.TRUE;
        }
        this.setMisionActual(categoriaActual.siguienteMision(misionActual));
        return Boolean.FALSE;
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