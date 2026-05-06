package ar.edu.utn.frba.ddsi.donaciones.models.entities.lugares;


public class Direccion {
  private String calleUno;
  private String calleDos;
  private int altura;
  private boolean sinAltura;
  private int piso;
  private int cuerpo;
  private String departamento;
  private Ciudad ciudad;

  /**
   * Constructor completo.
   */
  public Direccion(
      String calleUno,
      String calleDos,
      int altura,
      boolean sinAltura,
      int piso,
      int cuerpo,
      String departamento,
      Ciudad ciudad
  ) {
    this.calleUno = calleUno;
    this.calleDos = calleDos;
    this.altura = altura;
    this.sinAltura = sinAltura;
    this.piso = piso;
    this.cuerpo = cuerpo;
    this.departamento = departamento;
    this.ciudad = ciudad;
  }

  /**
   * Permite obtener la direccion completa.
   *
   * @return String con la direccion completa, incluyendo ciudad, provincia y pais.
   */
  public String getDireccion() {
    String valorAltura = sinAltura ? "S/N" : String.valueOf(altura);

    return String.format("%s %s y %s, Piso %d, Depto %s, Cuerpo %d, %s",
        calleUno,
        valorAltura,
        calleDos,
        piso,
        departamento,
        cuerpo,
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
}
