package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

public class PayloadEntregaDTO {
  private String fechaEntrega;
  private String horaEntrega;
  private String patenteCamion;
  private String nombreChofer;

  public PayloadEntregaDTO() {}

  public PayloadEntregaDTO(String fechaEntrega, String horaEntrega, String patenteCamion, String nombreChofer) {
    this.fechaEntrega = fechaEntrega;
    this.horaEntrega = horaEntrega;
    this.patenteCamion = patenteCamion;
    this.nombreChofer = nombreChofer;
  }

  public String getFechaEntrega() { return fechaEntrega; }
  public String getHoraEntrega() { return horaEntrega; }
  public String getPatenteCamion() { return patenteCamion; }
  public String getNombreChofer() { return nombreChofer; }
}