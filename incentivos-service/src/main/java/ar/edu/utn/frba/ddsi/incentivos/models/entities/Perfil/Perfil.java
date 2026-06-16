package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.TipoCategoria;
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
    private TipoCategoria categoriaActual;
    private List<Insignia> insignias;
    private Mision misionActual;
    private Integer posicionRanking;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.idPerfil = UUID.randomUUID();
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = TipoCategoria.COLABORADOR;
        this.insignias = new ArrayList<>();
        this.posicionRanking = null;
        this.misionActual = null; //se inicializa en personaService cuando se crea
    }

    public void otorgarInsignia(Insignia insignia) {
        insignia.setFechaObtencion(LocalDate.now());
        insignias.add(insignia);
    }

    public Perfil clonar() {
        Perfil copia = new Perfil(this.idUsuario, this.nombreUsuario);

        copia.setCategoriaActual(this.categoriaActual);
        copia.setInsignias(this.insignias);
        copia.setMisionActual(this.misionActual);

        return copia;
    }
}