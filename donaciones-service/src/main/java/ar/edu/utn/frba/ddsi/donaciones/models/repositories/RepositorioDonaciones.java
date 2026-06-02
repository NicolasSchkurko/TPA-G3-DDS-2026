package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estados;

import java.util.ArrayList;
import java.util.List;

public class RepositorioDonaciones {
    private final List<Donacion> donacionesNoAsignadas;
    private final List<Donacion> donacionesAsignadas;

    public RepositorioDonaciones() {
        this.donacionesNoAsignadas = new ArrayList<>();
        this.donacionesAsignadas = new ArrayList<>();
    }

    public void agregarDonacion(Donacion donacion) {
        donacionesNoAsignadas.add(donacion);
    }

    public void asignarDonacion(Donacion donacion) {
        if (donacionesNoAsignadas.contains(donacion)) {
            donacionesNoAsignadas.remove(donacion);
            donacion.setEstado(Estados.EN_DEPOSITO);
            donacionesAsignadas.add(donacion);
        }
    }
}
