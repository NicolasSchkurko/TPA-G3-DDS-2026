package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadBeneficiaria {
    private String razonSocial;
    private Direccion direccion;
    private Telefono nroTell;
    private List<Necesidad> necesidades;
    private MediosDeContacto correosRepresentantes;

    public EntidadBeneficiaria (String razonSoc, Direccion dir, Telefono nroTell, MediosDeContacto correosRepres){
        this.razonSocial = razonSoc;
        this.direccion = dir;
        this.nroTell = nroTell;
        this.necesidades = new ArrayList<Necesidad>();
        this.correosRepresentantes = correosRepres;
    }

    public void agregarNecesidad(Necesidad necesidad){
        this.necesidades.add(necesidad);
    }

    public List<Donacion> verDonaciones(){
        return necesidades.stream()
                .flatMap(necesidad -> necesidad.getDonaciones().stream())
                .toList();
    }

    public void confirmarRecepcion(Donacion donacion){


    }
}
