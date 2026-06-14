package ar.edu.utn.frba.ddsi.donaciones.models.entities.AlgoritmosDeAsignacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompatibilidadSemantica implements AlgoritmoAsignacion {

    @Override
    public List<EntidadBeneficiaria> rankear(Donacion donacion, List<EntidadBeneficiaria> entidades) {
        return entidades.stream()
                .filter(entidad -> mejorScore(entidad, donacion) > 0)
                .sorted((e1, e2) -> Double.compare(
                        mejorScore(e2, donacion),
                        mejorScore(e1, donacion)))
                .limit(10)
                .collect(Collectors.toList());
    }

    private double mejorScore(EntidadBeneficiaria entidad, Donacion donacion) {
        return entidad.getNecesidades().stream()
                .filter(n -> !n.estaSatisfecha())
                .filter(n -> n.getSubcategoria().equals(donacion.getSubcategoria()))
                .mapToDouble(n -> calcularScore(n, donacion))
                .max()
                .orElse(0);
    }

    private double calcularScore(Necesidad necesidad, Donacion donacion) {
        int cantidadFaltante = necesidad.getCantidadObjetivo() - necesidad.cantidadRecibida();
        int cantidadDonada = donacion.sumaCantidadBienes();
        return cantidadDonada <= cantidadFaltante
                ? (double) cantidadDonada / cantidadFaltante
                : (double) cantidadFaltante / cantidadDonada;
    }
}