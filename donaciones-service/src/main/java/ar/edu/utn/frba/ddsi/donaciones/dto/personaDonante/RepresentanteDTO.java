package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class RepresentanteDTO {
  private String nombre;
  private String apellido;
  private String numeroDeDocumento;
  private boolean activo;
  private int edad;
  private String genero;

}

