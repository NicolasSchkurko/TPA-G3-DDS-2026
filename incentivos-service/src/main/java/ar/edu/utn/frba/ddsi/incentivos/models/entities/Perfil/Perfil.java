package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

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
    private Integer totalDonaciones;
    private String nombreUsuario;
    private Categoria categoriaActual;
    private List<Insignia> insignias;
    private Mision misionActual;
    private List<MetricasActividad> metricas;
    private List<ActividadMensual> evolucionMensual;
    private Integer organizacionesAyudadas;
    private Integer posicionRanking;


    public Perfil(String nombreUsuario) {
        this(UUID.randomUUID(), nombreUsuario);
    }

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.totalDonaciones = 0;
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = Colaborador.getInstance();
        this.insignias = new ArrayList<>();
        this.metricas = new ArrayList<>();
        this.evolucionMensual = new ArrayList<>();
        this.organizacionesAyudadas = 0;
        this.posicionRanking = null;
        this.misionActual = categoriaActual.primeraMision();
    }

    public Perfil(UUID idUsuario) {
        this(idUsuario, null);
    }

    public void ascenderCategoria() {
        categoriaActual = categoriaActual.getSiguienteCategoria();
        misionActual = categoriaActual.primeraMision();
    }

    public void otorgarInsignia(Insignia insignia) {
        insignia.setFechaObtencion(LocalDate.now());
        insignias.add(insignia);
    }

    public void progresarMision(ImpactoDonacion donacion){
        misionActual.registrarProgreso(donacion);
        if(misionActual.estaCompleta()){
            this.otorgarInsignia(misionActual.getInsigniaObjetivo());
            if (categoriaActual.esUltimaMision(misionActual)) {
                this.ascenderCategoria();
            }
            else{
                misionActual = categoriaActual.siguienteMision(misionActual);
            }
        }
    }

//    public List<ActividadMensual> generarEvolucionMensual() {
//        YearMonth esteMes = YearMonth.now();
//        YearMonth mesPasado = esteMes.minusMonths(1);
//
//        // Si a futuro se quiere mostrar más meses (ej: los últimos 6),
//        // solo agregar más objetos ActividadMensual a esta lista.
//        List<ActividadMensual> evolucion = new ArrayList<>();
//        evolucion.add(new ActividadMensual(mesPasado, this.donaciones));
//        evolucion.add(new ActividadMensual(esteMes, this.donaciones));
//
//        return evolucion;
//    }
//
//    public MetricasActividad generarMetricasComparativas() {
//        YearMonth esteMes = YearMonth.now();
//        YearMonth mesPasado = esteMes.minusMonths(1);
//
//        ActividadMensual actActual = new ActividadMensual(esteMes, this.donaciones);
//        ActividadMensual actAnterior = new ActividadMensual(mesPasado, this.donaciones);
//
//        return new MetricasActividad(actActual, actAnterior);
//    }
}
