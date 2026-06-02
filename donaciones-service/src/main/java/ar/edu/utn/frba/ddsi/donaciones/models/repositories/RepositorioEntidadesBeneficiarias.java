package ar.edu.utn.frba.ddsi.donaciones.models.repositories;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;

public class RepositorioEntidadesBeneficiarias {
    private final List<EntidadBeneficiaria> entidades;
    //private final List<AlgoritmoAsignacion> algoritmos;

    public RepositorioEntidadesBeneficiarias() {
        this.entidades = new ArrayList<>();
        //this.algoritmos = new ArrayList<>();
    }

    public void agregarEntidad(EntidadBeneficiaria entidad) {
        entidades.add(entidad);
    }
}
