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

    public List<EntidadBeneficiaria> obtenerTodas() {
        return entidades;
    }
    
    public void agregarEntidad(EntidadBeneficiaria entidad) {
        if(!entidades.contains(entidad)){
            this.entidades.add(entidad);
        }
    }

    public EntidadBeneficiaria buscarPorRazonSocial(String razonSocial) {
        return entidades.stream()
                .filter(e -> e.getRazonSocial().equals(razonSocial))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad: " + razonSocial));
    }
}
