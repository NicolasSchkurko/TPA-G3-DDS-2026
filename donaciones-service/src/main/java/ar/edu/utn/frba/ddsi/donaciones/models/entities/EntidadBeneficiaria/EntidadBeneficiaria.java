package ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntidadBeneficiaria {
    private UUID id = UUID.randomUUID(); // Identificador único autogenerado
    private Direccion direccion;
    private List<Necesidad> necesidades;
    private Juridica personaJuridica;

    public EntidadBeneficiaria(Direccion dir, Juridica personaJuridica) {
        this.direccion = dir;
        this.necesidades = new ArrayList<>();
        this.personaJuridica = personaJuridica;
    }

    public void agregarNecesidad(Necesidad necesidad) {
        this.necesidades.add(necesidad);
    }

    public void eliminarNecesidad(Necesidad necesidad) {
        this.necesidades.remove(necesidad);
    }

    public Optional<Necesidad> buscarNecesidadPorId(UUID idNecesidad) {
        return necesidades.stream()
                          .filter(n -> n.getId().equals(idNecesidad))
                          .findFirst();
    }

    public List<Donacion> verDonaciones() {
        return necesidades.stream()
                          .flatMap(necesidad -> necesidad.getDonaciones().stream())
                          .toList();
    }


}