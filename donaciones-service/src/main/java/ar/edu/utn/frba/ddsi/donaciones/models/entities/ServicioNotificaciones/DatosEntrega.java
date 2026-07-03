package ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones;

public record DatosEntrega(
    String fechaEntrega,
    String horaEntrega,
    String patenteCamion,
    String nombreChofer
) {}