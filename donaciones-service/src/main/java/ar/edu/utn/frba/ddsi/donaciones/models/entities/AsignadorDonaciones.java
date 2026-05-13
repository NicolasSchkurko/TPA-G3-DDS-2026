package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignadorDonaciones {
    private List<EntidadBeneficiaria> entidades;

    public AsignadorDonaciones(List<EntidadBeneficiaria> entidades) {
        this.entidades = entidades;
    }

    public void asignarDonacion(Donacion donacion) {
        Optional<Necesidad> necesidadOptima = buscarNecesidadPendiente(donacion.getSubcategoria());

        if (necesidadOptima.isPresent()) {
            Necesidad necesidad = necesidadOptima.get();
            necesidad.registrarDonacionAsignada(donacion);
            donacion.setEstado(Estados.EN_DEPOSITO);
        }
    }

    private Optional<Necesidad> buscarNecesidadPendiente(SubcategoriaBien sub) {
        return entidades.stream()
                .flatMap(entidad -> entidad.getNecesidades().stream())
                .filter(necesidad -> necesidad.getSubcategoria().equals(sub))
                .filter(necesidad -> !necesidad.estaSatisfecha())
                .findFirst(); // Logica de prioridad, por ahora dejamos la primera en la lista
    }
}
