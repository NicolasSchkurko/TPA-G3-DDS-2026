package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import java.util.List;
import java.util.UUID;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaDonanteDTO {
  private UUID id;
  // Campo discriminador para saber qué subclase instanciar
  private String tipoPersona; // Valores válidos: "HUMANA" o "JURIDICA"

  // Campos comunes o de retorno
  private String nombreAMostrar;

  // Campos exclusivos de PersonaHumana
  private String nombre;
  private String apellido;
  private int edad;
  private int numeroDeDocumento;
  private String genero; // "HOMBRE", "MUJER", "OTRO"

  // Campos exclusivos de PersonaJuridica
  private String razonSocial;
  private String rubro;
  private String cuit;
  private String tipoJuridico; // "ONG", "EMPRESA", etc.

  // DTOs anidados
  private DireccionDTO direccion;
  private List<MediosContactoDTO> mediosDeContacto;
  private List<RepresentanteDTO> representantes;
}