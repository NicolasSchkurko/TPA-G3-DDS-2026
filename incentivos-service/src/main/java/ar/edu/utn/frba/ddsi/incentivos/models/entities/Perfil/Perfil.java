package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Colaborador;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
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
    private Integer posicionRanking;
    private Integer misionesCompletadasPeriodo;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.idPerfil = UUID.randomUUID();
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = Colaborador.getInstance();
        this.insignias = new ArrayList<>();
        this.posicionRanking = null;
        this.misionActual = categoriaActual.primeraMision();
        this.misionesCompletadasPeriodo = 0;
    }

    public void ascenderCategoria() {
        categoriaActual = categoriaActual.getSiguienteCategoria();
        misionActual = (categoriaActual != null) ? categoriaActual.primeraMision() : null;
    }

    public void otorgarInsignia(Insignia insignia) {
        insignia.setFechaObtencion(LocalDate.now());
        insignias.add(insignia);
    }

    //usar this o no es igual mientras no recibas un parametro con el mismo nombre del atributo
    public boolean progresarMision(ImpactoDonacion donacion){
        if (misionActual == null) {
            return false;
        }
        misionActual.evaluarDonacion(donacion);

        if (!misionActual.estaCompleta()) {
            return false;
        }

        otorgarInsignia(misionActual.getInsigniaObjetivo());
        this.misionesCompletadasPeriodo++;

        if (categoriaActual.esUltimaMision(misionActual)) {
            this.ascenderCategoria();
            return true;
        }
        misionActual = categoriaActual.siguienteMision(misionActual);
        return true;
    }
}