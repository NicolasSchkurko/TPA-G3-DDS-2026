package ar.edu.utn.frba.ddsi.donaciones.dto.logistica;

import java.util.List;

public class PayloadInicioRutaDTO {
  private List<String> items;
  private String urlRuta;

  public PayloadInicioRutaDTO() {}

  public List<String> getItems() { return items; }
  public String getUrlRuta() { return urlRuta; }
}