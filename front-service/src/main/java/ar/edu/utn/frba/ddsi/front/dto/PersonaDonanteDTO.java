package ar.edu.utn.frba.ddsi.front.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Espejo del PersonaDonanteDTO de donaciones-service. Se mantiene completo (y no sólo con los
 * campos que se listan en la tabla) porque la pantalla de administración renderiza el formulario
 * de edición del lado del servidor y necesita todos los datos modificables del donante.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaDonanteDTO {
    private UUID id;
    private Boolean activo;
    private String tipoPersona;

    // Persona humana
    private String nombre;
    private String apellido;
    private Integer edad;
    private Integer numeroDeDocumento;
    private String genero;

    // Persona jurídica
    private String razonSocial;
    private String rubro;
    private String cuit;
    private String tipoJuridico;

    // Contacto y domicilio
    private String nombreDeUsuario;
    private String correoElectronico;
    private String telefono;
    private MediosContactoDTO medioPredeterminado;
    private DireccionDTO direccion;
    private List<MediosContactoDTO> mediosDeContacto;
    private List<RepresentanteDTO> representantes;

    public boolean esJuridica() {
        return "JURIDICA".equalsIgnoreCase(tipoPersona);
    }

    /** Nombre para mostrar: razón social si es jurídica, nombre + apellido si es humana. */
    public String etiqueta() {
        if (razonSocial != null && !razonSocial.isBlank()) {
            return razonSocial;
        }
        String completo = ((nombre != null ? nombre : "") + " " + (apellido != null ? apellido : "")).trim();
        return completo.isBlank() ? "Sin nombre" : completo;
    }
}
