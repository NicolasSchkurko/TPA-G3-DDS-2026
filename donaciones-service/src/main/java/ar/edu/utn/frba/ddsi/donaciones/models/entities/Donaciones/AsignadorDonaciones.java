package ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.Notificador.Notificador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AsignadorDonaciones {
    private static AsignadorDonaciones instanciaUnica;

    private static List<Donacion> donacionesPendientes;
    private static List<EntidadBeneficiaria> entidades;

    private static Notificador notificador;// no sabemos como seria esto, tira warning

    private AsignadorDonaciones() {
        donacionesPendientes = new ArrayList<>();
        entidades = new ArrayList<>();
    }

    public static AsignadorDonaciones getInstance() {
        if (instanciaUnica == null) {
            instanciaUnica = new AsignadorDonaciones();
        }
        return instanciaUnica;
    }

    public static void asignarDonacion(Donacion donacion) {
        if (!entidades.isEmpty()) {
            for (EntidadBeneficiaria entidad : entidades) {
                for (Necesidad necesidad : entidad.getNecesidades()) {
                    if (necesidad.getSubcategoria().getNombre().equals(donacion.getSubcategoria().getNombre()) && necesidad.getSubcategoria().getCategoria().equals(donacion.getSubcategoria().getCategoria()) && !necesidad.estaSatisfecha()) {
                        necesidad.registrarDonacionAsignada(donacion);
                        donacion.setEntidad(entidad);
                        donacion.setEstado(Estados.EN_DEPOSITO);
                        //Se manda a la entida beneficiaria con el notificador (por ahora es un id aleatorio)
                        notificador.notificar(donacion.getEntidad().getId(), "Donacion asignada", "Se te asgino una donacion");
                        //Se manda a la persona donante con el notificador (por ahora es un id aleatorio)
                        notificador.notificar(donacion.getDonante().getId(), "Donacion asignada", "Se te asgino una donacion a una entidad");
                        return; //por ahora la primer necesidad que encuentre que coincida con la donacion sera satisfecha
                    }
                }
            }
        }
        agregarDonacion(donacion);
    }

    private static void agregarDonacion(Donacion donacion) {
        AsignadorDonaciones.getInstance();
        if (!donacionesPendientes.contains(donacion)) {
            donacionesPendientes.add(donacion);
        }
    }

    public static void reasignarDonacionesPendientes() {
        donacionesPendientes.forEach(AsignadorDonaciones::asignarDonacion);
    }


    public void agregarEntidad(EntidadBeneficiaria entidad) {
        AsignadorDonaciones.getInstance();
        if (!entidades.contains(entidad)) {
            entidades.add(entidad);
        }
    }

    public boolean estaRegistradaLaEntidad(String razonSocialBuscada) {
        if (entidades == null) {
            getInstance();
        }
        if (entidades == null || entidades.isEmpty() || razonSocialBuscada == null) {
            return false;
        }

        return entidades.stream().anyMatch(e -> e.getRazonSocial().equalsIgnoreCase(razonSocialBuscada));
    }

    public EntidadBeneficiaria buscarEntidadRegistrada(String razonSocialBuscada) {
        if (estaRegistradaLaEntidad(razonSocialBuscada)) {
            return entidades.stream().filter(e -> e.getRazonSocial().equalsIgnoreCase(razonSocialBuscada)).findAny().get();
        }
        return null;
    }
}