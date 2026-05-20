package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;


import lombok.Getter;
import lombok.Setter;


@Getter
@Setter

public class Direccion {

  private String calleUno;
  private String calleDos;
  private Integer altura;
  private int piso;
  private String departamento;
  private Ciudad ciudad;

  /**
   * Constructor completo.
   */
  public Direccion(
      String calleUno,
      String calleDos,
      Integer altura,
      int piso,
      String departamento,
      Ciudad ciudad
  ) {
    this.calleUno = calleUno;
    this.calleDos = calleDos;
    this.altura = altura;
    this.piso = piso;
    this.departamento = departamento;
    this.ciudad = ciudad;
  }

  /**
   * Constructor sin altura.
   */
  public Direccion(
      String calleUno,
      String calleDos,
      int piso,
      String departamento,
      Ciudad ciudad
  ) {
    this.calleUno = calleUno;
    this.calleDos = calleDos;
    this.altura = null;
    this.piso = piso;
    this.departamento = departamento;
    this.ciudad = ciudad;
  }

  /**
   * Permite obtener la direccion completa.
   *
   * @return String con la direccion completa, incluyendo ciudad, provincia y pais.
   */
  public String getDireccion() {
    String valorAltura = altura == null ? "S/N" : String.valueOf(altura);

    return String.format("%s %s y %s, Piso %d, Depto %s, Cuerpo %d, %s",
        calleUno,
        valorAltura,
        calleDos,
        piso,
        departamento,
        ciudad.getDireccion()
    );
  }

  // Esto es si o si con consulta a api (openmaps u otro) no se puede hacer ahora
  public String getLatitud() {
    return "INSERTAR CONTENIDO A DEVOLVER";
  }
  public String getLongitud() {
    return "INSERTAR CONTENIDO A DEVOLVER";
  }

  // No existe el condicional que define si esta o no habilitado
  public boolean estaHabilitadaEnvio() {
    return true;
  }

  @Override
  public String toString() {
    return "Direccion{ciudad=" + ciudad + ", departamento=" + departamento + ", piso=" + piso + ", altura=" + altura + ", calleUno=" + calleUno + ", calleDos=" + calleDos + '}';
  }
}
