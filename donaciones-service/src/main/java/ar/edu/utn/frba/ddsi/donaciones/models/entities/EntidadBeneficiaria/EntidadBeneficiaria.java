package ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
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
        this.necesidades = new ArrayList<>();
        this.correosRepresentantes = correosRepres;
    }

    public void agregarNecesidad(Necesidad necesidad){
        this.necesidades.add(necesidad);
    }

    public void eliminarNecesidad(Necesidad necesidad) {
        this.necesidades.remove(necesidad);
    }

    public List<Donacion> verDonaciones(){
        return necesidades.stream()
                .flatMap(necesidad -> necesidad.getDonaciones().stream())
                .toList();
    }

    public void confirmarRecepcion(Donacion donacion){

    }

    @Override
    public String toString() {
        return "EntidadBeneficiaria{razonSocial=" + razonSocial + ", direccion=" + direccion + ", telefono=" + nroTell + ", necesidades=" + necesidades + ", correosRepresentantes=" + correosRepresentantes + '}';
    }
}
