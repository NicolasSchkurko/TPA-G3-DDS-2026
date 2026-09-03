package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.ReglaConstancia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.PosicionRanking;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
    private Mision mision;
    private List<ImpactoDonacion> donacionesExitosas;
    private Integer progreso;

    public ProgresoMision(Mision mision) {
        this.mision = mision;
        this.donacionesExitosas = new ArrayList<>();
        this.progreso = 0;
    }

    public void evaluarConstancia() {
        ReglaConstancia constancia = mision.getReglaDeProgreso().getConstancia();
        if (constancia == null || donacionesExitosas.isEmpty()) return;

        LocalDateTime limite = donacionesExitosas.getLast().getFechaEntrega().plus(
                constancia.getCantidad(), constancia.getUnidadTiempo());

        if (LocalDateTime.now().isAfter(limite)) {
            donacionesExitosas.clear();
            progreso = 0;
        }
    }

    public boolean estaCompleta() {
        return mision.getReglaDeProgreso().estaCompleta(progreso);
    }

    public void evaluarProgreso(ImpactoDonacion donacion) {
        Object valorAtributo = mision.getReglaDeProgreso().aplicar(donacion);
        if (mision.getReglaDeProgreso().operar(valorAtributo)) {
            donacionesExitosas.add(donacion);
            progreso++;
        }
    }

    public Insignia progresarMision (ImpactoDonacion donacion, PosicionRanking posicionRanking) {
        evaluarConstancia();
        evaluarProgreso(donacion);
        if (this.estaCompleta()) {
            posicionRanking.incrementarMisionesCumplidas();
            return this.getMision().getInsigniaObjetivo();
        }
        return null;
    }


}
