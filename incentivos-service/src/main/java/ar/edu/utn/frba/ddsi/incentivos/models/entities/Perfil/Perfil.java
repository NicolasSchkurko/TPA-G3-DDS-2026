package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria.COLABORADOR;

@Getter
@Setter
public class Perfil {
    private UUID idUsuario; // id en donaciones
    private UUID idPerfil; // id interno
    private String nombreUsuario;
    private TipoCategoria categoriaActual;
    private List<Insignia> insignias;
    private Mision misionActual;
    private PosicionRanking posicionRanking;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.idPerfil = UUID.randomUUID();
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = COLABORADOR;
        this.insignias = new ArrayList<>();
        // Inicializamos la posición en el ranking con valores por defecto
        this.posicionRanking = new PosicionRanking(null, this.idPerfil, this.idUsuario, this.nombreUsuario, 0);
        this.misionActual = null; //se inicializa en personaService cuando se crea
    }

    public void progresarMision(ImpactoDonacion donacion){
        misionActual.evaluarDonacion(donacion);

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
        if (this.posicionRanking == null) {
            this.posicionRanking = new PosicionRanking(null, this.idPerfil, this.idUsuario, this.nombreUsuario, 0);
        }
        Integer current = this.posicionRanking.getMisionesCumplidasEnPeriodo();
        this.posicionRanking.setMisionesCumplidasEnPeriodo((current == null ? 1 : current + 1));
    }

    public Perfil clonar() {
        Perfil copia = new Perfil(this.idUsuario, this.nombreUsuario);

        copia.setCategoriaActual(this.categoriaActual);
        copia.setInsignias(this.insignias);
        copia.setMisionActual(this.misionActual);

        return copia;
    }
}