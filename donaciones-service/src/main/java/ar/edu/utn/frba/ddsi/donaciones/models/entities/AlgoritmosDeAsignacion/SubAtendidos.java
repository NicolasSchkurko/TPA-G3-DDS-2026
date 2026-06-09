package ar.edu.utn.frba.ddsi.donaciones.models.entities.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;

public class SubAtendidos implements AlgoritmoAsignacion {

    @Override
    public List<EntidadBeneficiaria> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        // TODO: ordenar entidades según cuáles recibieron menos donaciones en el último trimestre
        return new ArrayList<>();
    }
}
