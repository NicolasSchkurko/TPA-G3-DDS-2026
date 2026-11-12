package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.EntidadPropia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.TipoEventoNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeNecesidades;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GestorAsignaciones {
    private RepositorioDeDonaciones repositorioDonaciones;
    private RepositorioDeNecesidades repositorioNecesidades;
    private FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion;

    public GestorAsignaciones(RepositorioDeDonaciones repositorioDonaciones,
                              RepositorioDeNecesidades repositorioNecesidades,
                              FabricaEstrategiasNotificacion fabricaEstrategiasNotificacion) {
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioNecesidades = repositorioNecesidades;
        this.fabricaEstrategiasNotificacion = fabricaEstrategiasNotificacion;
    }

    public void asignarPropuesta(Donacion donacion, PropuestaAsignacion  propuesta) {
        if (donacion != null && propuesta != null && propuesta.getEntidad() != null
                && EntidadPropia.esPropia(donacion.getDonante(), propuesta.getEntidad())) {
            throw new IllegalArgumentException(
                    "No se puede asignar la donación a la entidad beneficiaria propia del donante (mismo usuario).");
        }
        asignarEntidad(donacion.getId(), propuesta.getEntidad());
        agregarDonacionANecesidad(propuesta.getNecesidad().getId(), donacion);
    }

    public Donacion cambiarEstado(UUID id, String nuevoEstado, String justificacion) {
        Donacion donacion = repositorioDonaciones.obtenerPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Donación no encontrada con ID: " + id));

        Estado estado = parseEstado(nuevoEstado);

        donacion.actualizarEstado(estado, justificacion);

        // Las acciones posteriores van ANTES de guardar: si fallan (por ejemplo porque la
        // donación todavía no tiene entidad asignada), el estado nuevo no debe quedar
        // persistido. Antes el guardar pasaba primero y una falla acá dejaba el cambio de
        // estado ya commiteado mientras el controller, al atrapar la excepción como
        // RuntimeException, respondía 404 (como si la donación no existiera).
        procesarAccionesPostCambioEstado(donacion, estado, nuevoEstado);

        repositorioDonaciones.guardar(donacion);

        return donacion;
    }

    private Estado parseEstado(String nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }

        switch (nuevoEstado.trim().toUpperCase()) {
            case "EN_DEPOSITO":
                return Estado.EN_DEPOSITO;
            case "PENDIENTE_ASIGNACION":
                return Estado.PENDIENTE_ASIGNACION;
            case "ASIGNADO":
                return Estado.ASIGNADO;
            case "LISTA_PARA_ENTREGAR":
                return Estado.LISTA_PARA_ENTREGAR;
            case "EN_TRASLADO":
                return Estado.EN_TRASLADO;
            case "ENTREGADO":
                return Estado.ENTREGADO;
            case "ENTREGA_FALLIDA":
                return Estado.ENTREGA_FALLIDA;
            case "VENCIDO":
                return Estado.VENCIDO;
            default:
                throw new IllegalArgumentException("Estado desconocido: " + nuevoEstado);
        }
    }

    private void procesarAccionesPostCambioEstado(Donacion donacion, Estado estado, String nuevoEstado) {
        boolean esAsignado = "ASIGNADO".equalsIgnoreCase(nuevoEstado) || estado == Estado.ASIGNADO;
        if (!esAsignado) return;

        if (donacion.getEntidad() == null) {
            throw new IllegalArgumentException(
                    "No se puede marcar la donación " + donacion.getId() + " como ASIGNADO: todavía no tiene una entidad beneficiaria asignada.");
        }
        if (donacion.getSubcategoria() == null) {
            throw new IllegalArgumentException(
                    "No se puede marcar la donación " + donacion.getId() + " como ASIGNADO: no tiene subcategoría.");
        }

        // A incentivos se le reporta recién al ENTREGAR (GestorEventosLogistica), no al asignar:
        // sus reglas de misión cuentan donaciones "ENTREGADA", y una asignación todavía no lo es.
        fabricaEstrategiasNotificacion.ejecutar(TipoEventoNotificacion.DONACION_ASIGNADA, donacion);
    }

    private void asignarEntidad(UUID donacionId, EntidadBeneficiaria entidad) {
        java.util.Optional<Donacion> donacionOpt = repositorioDonaciones.obtenerPorId(donacionId);
        Donacion donacion = donacionOpt == null ? null : donacionOpt.orElse(null);
        if (donacion != null && EntidadPropia.esPropia(donacion.getDonante(), entidad)) {
            throw new IllegalArgumentException(
                    "No se puede asignar la donación a la entidad beneficiaria propia del donante (mismo usuario).");
        }
        try {
            repositorioDonaciones.asignarEntidad(donacionId, entidad);
            System.out.println("Entidad asignada con éxito a la donación: " + donacionId);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al asignar entidad: " + e.getMessage());
        }
    }

    //Lo agregué para poder registrar donaciones
    private void agregarDonacionANecesidad(UUID necesidadId, Donacion donacion) {
        try {
            Necesidad necesidad = repositorioNecesidades.findById(necesidadId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "No se encontró la Necesidad con ID: " + necesidadId));
            necesidad.registrarDonacionAsignada(donacion);
            repositorioDonaciones.guardar(donacion);
            System.out.println("Donación registrada con éxito en la necesidad: " + necesidadId);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al registrar donación en necesidad: " + e.getMessage());
        }
    }

}
