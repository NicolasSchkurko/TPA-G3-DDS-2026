package ar.edu.utn.frba.ddsi.donaciones.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaDonanteDTO {
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

  // agregar dto de Direccion, MediosDeContacto y Representantes.
}

