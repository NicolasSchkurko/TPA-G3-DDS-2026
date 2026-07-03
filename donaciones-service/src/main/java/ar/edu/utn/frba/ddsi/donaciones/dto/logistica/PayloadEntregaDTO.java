package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

public class PayloadEntregaDTO {
  private String fechaEntrega;
  private String horaEntrega;
  private String patenteCamion;
  private String nombreChofer;

  public PayloadEntregaDTO() {}

  public String getFechaEntrega() { return fechaEntrega; }
  public String getHoraEntrega() { return horaEntrega; }
  public String getPatenteCamion() { return patenteCamion; }
  public String getNombreChofer() { return nombreChofer; }
}