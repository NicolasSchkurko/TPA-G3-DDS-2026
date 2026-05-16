package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsignadorDonaciones {
    private static AsignadorDonaciones instanciaUnica;

    private List<EntidadBeneficiaria> entidades;

    private AsignadorDonaciones() {
        this.entidades = new ArrayList<>();
    }

    public static AsignadorDonaciones getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new AsignadorDonaciones();
        }
        return instanciaUnica;
    }

    public void asignarDonacion(Donacion donacion) {
        for (EntidadBeneficiaria entidad : entidades) {
            for (Necesidad necesidad : entidad.getNecesidades()) {
                if (necesidad.getSubcategoria().equals(donacion.getSubcategoria()) && !necesidad.estaSatisfecha()) {
                    necesidad.registrarDonacionAsignada(donacion);
                    donacion.setEntidad(entidad);
                    donacion.setEstado(Estados.EN_DEPOSITO);
                    return; //por ahora la primer necesidad que encuentre que coincida con la donacion sera satisfecha
                }
            }
        }
        //si la donacion no entro al bucle seguira sin asignarse
    }
}
