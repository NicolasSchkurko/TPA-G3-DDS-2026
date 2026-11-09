package ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Direccion {

  @Id
  private UUID id = UUID.randomUUID();

  private String calleUno;
  private String calleDos;
  private Integer altura;
  private int piso;
  private String departamento;

  @ManyToOne
  @JoinColumn(name = "ciudad_id", referencedColumnName = "id")
  private Ciudad ciudad;

  /**
   * Coordenadas geocodificadas por Georef. Nullable: las filas viejas y las direcciones que
   * Georef no pudo resolver no tienen coordenadas.
   */
  private Double latitud;
  private Double longitud;

  /**
   * Control de reintentos del cron de geocoding. No son datos de negocio: existen para no
   * consultar Georef todas las noches por una dirección que nunca va a resolverse.
   */
  private LocalDateTime ultimoIntentoGeocoding;
  private int intentosGeocoding;

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

  public void anonimizar() {
    this.calleUno = null;
    this.calleDos = null;
    this.altura = null;
    this.piso = 0;
    this.departamento = null;
    // Las coordenadas también identifican el domicilio: se borran con el resto (Punto 75).
    this.latitud = null;
    this.longitud = null;
    this.ultimoIntentoGeocoding = null;
    this.intentosGeocoding = 0;
  }

  public boolean tieneCoordenadas() {
    return latitud != null && longitud != null;
  }

  /**
   * Compara la ubicación textual (no las coordenadas) para decidir si un PUT cambió la
   * dirección o solo otros datos del donante/entidad. Si no cambió, se conservan las
   * coordenadas existentes y no se vuelve a llamar a Georef.
   */
  public boolean mismaDireccion(Direccion otra) {
    if (otra == null) {
      return false;
    }
    return Objects.equals(normalizar(calleUno), normalizar(otra.calleUno))
        && Objects.equals(normalizar(calleDos), normalizar(otra.calleDos))
        && Objects.equals(altura, otra.altura)
        && piso == otra.piso
        && Objects.equals(normalizar(departamento), normalizar(otra.departamento))
        && mismaCiudad(ciudad, otra.ciudad);
  }

  private static boolean mismaCiudad(Ciudad una, Ciudad otra) {
    if (una == otra) {
      return true;
    }
    if (una == null || otra == null) {
      return false;
    }
    if (una.getId() != null && una.getId().equals(otra.getId())) {
      return true;
    }
    return Objects.equals(normalizar(una.getNombre()), normalizar(otra.getNombre()))
        && mismaProvincia(una.getProvincia(), otra.getProvincia());
  }

  private static boolean mismaProvincia(Provincia una, Provincia otra) {
    if (una == otra) {
      return true;
    }
    if (una == null || otra == null) {
      return false;
    }
    if (una.getId() != null && una.getId().equals(otra.getId())) {
      return true;
    }
    return Objects.equals(normalizar(una.getNombre()), normalizar(otra.getNombre()))
        && mismoPais(una.getPais(), otra.getPais());
  }

  private static boolean mismoPais(Pais uno, Pais otro) {
    if (uno == otro) {
      return true;
    }
    if (uno == null || otro == null) {
      return false;
    }
    return Objects.equals(normalizar(uno.getNombre()), normalizar(otro.getNombre()));
  }

  private static String normalizar(String valor) {
    return valor == null ? null : valor.trim().toLowerCase(java.util.Locale.ROOT);
  }

  /**
   * Permite obtener la direccion completa.
   *
   * @return String con la direccion completa, incluyendo ciudad, provincia y pais.
   */
  public String getDireccion() {
    String valorAltura = altura == null ? "S/N" : String.valueOf(altura);

    return String.format("%s %s y %s, Piso %d, Depto %s, %s",
            calleUno,
            valorAltura,
            calleDos,
            piso,
            departamento,
            ciudad.getDireccion()
    );
  }

  // No hay todavía una condición de negocio que lo decida.
  public boolean estaHabilitadaEnvio() {
    return true;
  }

  @Override
  public String toString() {
    return "Direccion{ciudad=" + ciudad + ", departamento=" + departamento + ", piso=" + piso + ", altura=" + altura + ", calleUno=" + calleUno + ", calleDos=" + calleDos + '}';
  }
}
