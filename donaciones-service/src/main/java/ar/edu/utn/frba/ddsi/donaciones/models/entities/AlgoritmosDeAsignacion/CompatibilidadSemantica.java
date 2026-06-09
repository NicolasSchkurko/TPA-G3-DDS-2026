package ar.edu.utn.frba.ddsi.donaciones.models.entities.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;

public class CompatibilidadSemantica implements AlgoritmoAsignacion {

    @Override
    public List<EntidadBeneficiaria> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        // TODO: ordenar entidades según qué tan bien matchean sus necesidades con la donacion
        return new ArrayList<>();
    }
}
