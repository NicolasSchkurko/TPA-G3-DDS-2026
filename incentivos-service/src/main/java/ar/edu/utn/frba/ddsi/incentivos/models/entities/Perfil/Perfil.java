package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categorias.Colaborador;
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
    private Integer posicionRanking;

    public Perfil(UUID idUsuario, String nombreUsuario) {
        this.idUsuario = idUsuario;
        this.idPerfil = UUID.randomUUID();
        this.nombreUsuario = nombreUsuario;
        this.categoriaActual = Colaborador.getInstance();
        this.insignias = new ArrayList<>();
        this.posicionRanking = null;
        this.misionActual = categoriaActual.primeraMision();
    }

    public void ascenderCategoria() {
        categoriaActual = categoriaActual.getSiguienteCategoria();
        misionActual = categoriaActual.primeraMision();
    }

    public void otorgarInsignia(Insignia insignia) {
        insignia.setFechaObtencion(LocalDate.now());
        insignias.add(insignia);
    }

    //usar this o no es igual mientras no recibas un parametro con el mismo nombre del atributo
    public void progresarMision(ImpactoDonacion donacion){
        if (misionActual == null) {
            return;
        }
        misionActual.evaluarDonacion(donacion);

        if (!misionActual.estaCompleta()) {
            return;
        }

        otorgarInsignia(misionActual.getInsigniaObjetivo());

        if (categoriaActual.esUltimaMision(misionActual)) {
            categoriaActual = categoriaActual.getSiguienteCategoria();

            misionActual = (categoriaActual != null) ? categoriaActual.primeraMision() : null;
            return;
        }
        misionActual = categoriaActual.siguienteMision(misionActual);
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
