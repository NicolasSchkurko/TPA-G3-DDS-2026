package ar.edu.utn.frba.ddsi.donaciones.models.entities.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.util.List;

public interface AlgoritmoAsignacion {
    List<EntidadBeneficiaria> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades);
}
