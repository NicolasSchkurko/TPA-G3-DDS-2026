package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignia.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.Optional;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @OneToMany(cascade = CascadeType.ALL)
    private List<ImpactoDonacion> donaciones;

    private Integer progreso;

    public ProgresoMision(Mision mision) {
        this.mision = mision;
        this.donaciones = new ArrayList<>();
        this.progreso = 0;
    }

    public void evaluarConstancia() {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null || donaciones.isEmpty()) return;

        Optional<ImpactoDonacion> ultimaExitosa = donaciones.stream()
                                                            .filter(d -> Boolean.TRUE.equals(d.getExito()))
                                                            .reduce((first, second) -> second); // reduce to the last element

        if (ultimaExitosa.isEmpty()) return; // no successful donation -> nothing to evaluate

        LocalDateTime limite = ultimaExitosa.get().getFechaEntrega()
                                            .plus(constancia.getCantidad(), constancia.getUnidadTiempo());

        if (LocalDateTime.now().isAfter(limite)) {
            donaciones.clear();
            progreso = 0;
        }
    }

    public boolean estaCompleta() {
        return mision.getReglaDeProgreso().estaCompleta(progreso);
    }

    public void evaluarProgreso(ImpactoDonacion donacion) {
        Object valorAtributo = mision.getReglaDeProgreso().aplicar(donacion);
        if (mision.getReglaDeProgreso().operar(valorAtributo)) {
            progreso++;
        }
        donaciones.add(donacion);
    }

    // Se ha removido PosicionRanking de los parámetros.
    // Esa actualización debe manejarse mediante un EventListener que escuche MisionCompletada.
    public Insignia progresarMision(ImpactoDonacion donacion) {
        evaluarConstancia();
        evaluarProgreso(donacion);

        if (this.estaCompleta()) {
            return this.getMision().getInsigniaObjetivo();
        }
        return null;
    }
}