package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProgresoMision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Mision mision;

    private Integer progreso;

    public ProgresoMision(Mision mision) {
        this.mision = mision;
        this.progreso = 0;
    }

    public void evaluarConstancia(List<ImpactoDonacion> donaciones,
                                   LocalDateTime fechaEvaluacion) {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null) return;
        if (donaciones.isEmpty()) {
            progreso = 0;
            return;
        }

        int progresoActual = 0;
        LocalDateTime anterior = null;

//las donaciones a evaluar constancia deben ser las que hayan hecho progresar la mision actual
        List<ImpactoDonacion> donacionesEvaluar = donaciones.stream()
                .filter(donacion -> Boolean.TRUE.equals(
                        donacion.getHizoProgresarMision())
                )
                .toList();

        for (ImpactoDonacion donacion : donacionesEvaluar) {
            LocalDateTime limite = anterior == null
                    ? null
                    : anterior.plus(constancia.getCantidad(), constancia.getUnidadTiempo());

            if (limite != null && donacion.getFechaEntrega().isAfter(limite)) {
                progresoActual = 0;
            }

            progresoActual++;
            anterior = donacion.getFechaEntrega();
        }

        LocalDateTime limite = anterior == null
                ? null
                : anterior.plus(constancia.getCantidad(), constancia.getUnidadTiempo());
        progreso = limite != null && fechaEvaluacion.isAfter(limite)
                ? 0
                : progresoActual;
    }

    public boolean estaCompleta() {
        return mision.getReglaDeProgreso().estaCompleta(progreso);
    }

    public boolean evaluarProgreso(ImpactoDonacion donacion) {
        donacion.setIdMision(mision.getIdMision());
        Object valorAtributo = mision.getReglaDeProgreso().aplicar(donacion);
        boolean hizoProgresar = mision.getReglaDeProgreso().operar(valorAtributo);
        donacion.setHizoProgresarMision(hizoProgresar);
        return hizoProgresar;
    }

    // Se ha removido PosicionRanking de los parámetros.
    // Esa actualización debe manejarse mediante un EventListener que escuche MisionCompletada.
    public Insignia progresarMision(ImpactoDonacion donacion,
                                    List<ImpactoDonacion> donaciones) {
        boolean hizoProgresar = evaluarProgreso(donacion); //la donacion se

        if (mision.getReglaDeProgreso().getConstancia() != null) {
            List<ImpactoDonacion> donacionesEvaluar = new ArrayList<>(donaciones);
            donacionesEvaluar.add(donacion);
            donacionesEvaluar.sort(Comparator.comparing(ImpactoDonacion::getFechaEntrega));
            evaluarConstancia(donacionesEvaluar, donacion.getFechaEntrega());
        } else if (hizoProgresar) {
            progreso++;
        }

        if (this.estaCompleta()) {
            return this.getMision().getInsigniaObjetivo();
        }
        return null;
    }
}