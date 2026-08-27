package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class PersonaDonanteDTO {
  private UUID id;
  private String tipoPersona;
  private String nombreAMostrar;
  private String nombre;
  private String apellido;
  private int edad;
  private int numeroDeDocumento;
  private String genero;
  private String razonSocial;
  private String rubro;
  private String cuit;
  private String tipoJuridico;
  private MediosContactoDTO medioPredeterminado;
  private DireccionDTO direccion;
  private List<MediosContactoDTO> mediosDeContacto;
  private List<RepresentanteDTO> representantes;

  public Donante toDomain() {
    Direccion dir = this.direccion != null ? this.direccion.toDomain() : null;

    Persona persona = crearPersonaDesdeTipo();
    List<MedioDeContacto> medios = mapearMedios();
    MedioDeContacto medioPredeterminado = resolverMedioPredeterminado(medios);

    persona.getMediosDeContacto().agregarMediosDeContacto(medios);
    persona.getMediosDeContacto().setMedioDeContactoPredeterminado(medioPredeterminado);

    if (persona instanceof Juridica juridica && this.representantes != null) {
      List<Representante> representantes = this.representantes.stream()
          .map(RepresentanteDTO::toDomain)
          .collect(Collectors.toList());
      juridica.agregarRepresentantes(representantes);
    }

    return new Donante(dir, persona);
  }

  public static PersonaDonanteDTO from(Donante entidad) {
    if (entidad == null) return null;

    PersonaDonanteDTO dto = new PersonaDonanteDTO();
    dto.setId(entidad.getId());
    dto.setDireccion(DireccionDTO.from(entidad.getDireccion()));

    if (entidad.getPersona() == null) {
      return dto;
    }

    Persona persona = entidad.getPersona();
    dto.setNombreAMostrar(persona.getNombreDeUsuario());

    if (persona.getMediosDeContacto() != null) {
      List<MedioDeContacto> lista = persona.getMediosDeContacto().getListaMediosDeContacto();
      if (lista != null) {
        dto.setMediosDeContacto(
            lista.stream()
                .filter(java.util.Objects::nonNull)
                .map(MediosContactoDTO::from)
                .collect(Collectors.toList())
        );
      }

      MedioDeContacto predeterminado = persona.getMediosDeContacto().getMedioDeContactoPredeterminado();
      if (predeterminado != null) {
        dto.setMedioPredeterminado(MediosContactoDTO.from(predeterminado));
      }
    }

    if (persona instanceof Humana humana) {
      mapearHumana(dto, humana);
      return dto;
    }

    if (persona instanceof Juridica juridica) {
      mapearJuridica(dto, juridica);
      return dto;
    }

    dto.setTipoPersona("DESCONOCIDO");
    return dto;
  }

  private Persona crearPersonaDesdeTipo() {
    String tipo = this.tipoPersona != null ? this.tipoPersona.toUpperCase() : "HUMANA";

    if ("HUMANA".equals(tipo)) {
      Genero genero = this.generoValido() ? Genero.valueOf(this.genero.toUpperCase()) : Genero.OTRO;
      return new Humana(this.nombre, this.apellido, this.edad, this.numeroDeDocumento, genero, this.nombreAMostrar);
    }

    if ("JURIDICA".equals(tipo)) {
      TipoJuridico tipoJuridico = this.tipoJuridicoValido()
          ? TipoJuridico.valueOf(this.tipoJuridico.toUpperCase())
          : TipoJuridico.ONG;

      return new Juridica(this.razonSocial, this.rubro, tipoJuridico, this.cuit, new ArrayList<>(), this.nombreAMostrar);
    }

    throw new IllegalArgumentException("Tipo de persona inválido: " + this.tipoPersona);
  }

  private List<MedioDeContacto> mapearMedios() {
    List<MedioDeContacto> medios = new ArrayList<>();
    if (this.mediosDeContacto == null) return medios;

    for (MediosContactoDTO dto : this.mediosDeContacto) {
      MedioDeContacto medio = dto.toDomain();
      if (medio != null) {
        medios.add(medio);
      }
    }
    return medios;
  }

  private MedioDeContacto resolverMedioPredeterminado(List<MedioDeContacto> medios) {
    if (medios.isEmpty()) return null;

    if (this.medioPredeterminado == null) {
      return medios.get(0);
    }

    for (MedioDeContacto medio : medios) {
      if (medio.getTipo().equalsIgnoreCase(this.medioPredeterminado.getTipo())
          && medio.getValor().equalsIgnoreCase(this.medioPredeterminado.getValor())) {
        return medio;
      }
    }

    return medios.get(0);
  }

  private boolean generoValido() {
    return this.genero != null && !this.genero.isBlank();
  }

  private boolean tipoJuridicoValido() {
    return this.tipoJuridico != null && !this.tipoJuridico.isBlank();
  }

  private static void mapearHumana(PersonaDonanteDTO dto, Humana humana) {
    dto.setTipoPersona("HUMANA");
    dto.setNombre(humana.getNombre());
    dto.setApellido(humana.getApellido());
    dto.setEdad(humana.getEdad());
    dto.setNumeroDeDocumento(humana.getNumeroDeDocumento());
    dto.setGenero(humana.getGenero() != null ? humana.getGenero().name() : null);
  }

  private static void mapearJuridica(PersonaDonanteDTO dto, Juridica juridica) {
    dto.setTipoPersona("JURIDICA");
    dto.setRazonSocial(juridica.getRazonSocial());
    dto.setRubro(juridica.getRubro());
    dto.setCuit(juridica.getCuit());
    dto.setTipoJuridico(juridica.getTipoJuridico() != null ? juridica.getTipoJuridico().name() : null);

    if (juridica.getRepresentantes() != null) {
      dto.setRepresentantes(
          juridica.getRepresentantes().stream()
              .filter(java.util.Objects::nonNull)
              .map(RepresentanteDTO::from)
              .collect(Collectors.toList())
      );
    }
  }
}