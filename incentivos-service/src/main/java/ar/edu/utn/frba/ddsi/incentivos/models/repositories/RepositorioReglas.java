package ar.edu.utn.frba.ddsi.incentivos.models.repositories;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.MotorDeReglas.*;

import java.util.ArrayList;
import java.util.List;

public class RepositorioReglas {
    private final List<Regla> reglas;

    private RepositorioReglas() {
        this.reglas = new ArrayList<>();
        inicializarReglasBase();
    }

    public void inicializarReglasBase() {
        // Registras las reglas segun atributos impactoDonacion
        reglas.add(new ReglaFechaEntrega());
        reglas.add(new ReglaCantidadBienes());
        reglas.add(new ReglaSubCategoria());
        reglas.add(new ReglaCategoria());
        reglas.add(new ReglaEntidadBeneficiaria());
        reglas.add(new ReglaEstado());
    }

    public List<Regla> obtenerTodas() {
        return new ArrayList<>(this.reglas);
    }
}
