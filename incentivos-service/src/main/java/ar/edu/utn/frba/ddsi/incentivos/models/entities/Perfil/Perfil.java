package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.dto.ActividadMensualDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.MetricasActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.ActividadMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Graficos.MetricasActividad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria.*;
import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.TipoCategoria.*;

@Getter
@Setter
public class Perfil {
    //TODO agregar los atributos necesarios que vengan del repositorio
    private UUID idUsuario;
    private String nombreUsuario;
    private Categoria categoriaActual;
    private List<Insignia> insignias;
    private Mision misionActual;
    private List<Donacion> donaciones;
    private List<MetricasActividad> metricas;
    private List<ActividadMensual> evolucionMensual;
    private Integer organizacionesAyudadas;
    private Integer posicionRanking;


    public Perfil(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = Colaborador.getInstance();
        this.insignias = new ArrayList<>();
        this.misionActual = categoriaActual.primeraMision();
    }

    public void ascenderCategoria() {
        categoriaActual = categoriaActual.getSiguienteCategoria();
        misionActual = categoriaActual.primeraMision();
    }

    public void otorgarInsignia(Insignia insignia) {
        insignia.setFecha(LocalDate.now());
        insignias.add(insignia);
    }

    public void misionCompletada(){
        if(misionActual.completarMision(this)){
            this.otorgarInsignia(misionActual.getInsignia());
            if (categoriaActual.esUltimaMision(misionActual)) {
                this.ascenderCategoria();
            }
            else{
                misionActual = categoriaActual.siguienteMision(misionActual);
            }
        }
    }

    public Integer totalDonaciones(){
        return this.donaciones.toArray().length;
    }
}
