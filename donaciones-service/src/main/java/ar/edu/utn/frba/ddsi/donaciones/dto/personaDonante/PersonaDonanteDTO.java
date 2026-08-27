package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
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
    Direccion dir = (this.direccion != null) ? this.direccion.toDomain() : null;
    List<MedioDeContacto> medios = new ArrayList<>();
    MedioDeContacto medioPred = null;

    if (this.mediosDeContacto != null) {
      for (MediosContactoDTO medioDTO : this.mediosDeContacto) {
        MedioDeContacto m = medioDTO.toDomain();
        if (m != null) {
          medios.add(m);
          if (medioPred == null) medioPred = m;
          if (this.medioPredeterminado != null && m.getTipo().equalsIgnoreCase(this.medioPredeterminado.getTipo()) && m.getValor().equalsIgnoreCase(this.medioPredeterminado.getValor())) {
            medioPred = m;
          }
        }
      }
    }
    if (medioPred == null && !medios.isEmpty()) medioPred = medios.get(0);
    else if (medioPred == null) throw new IllegalArgumentException("Se requiere un medio de contacto.");

    String tipoPer = (this.tipoPersona != null) ? this.tipoPersona.toUpperCase() : "HUMANA";

    if ("HUMANA".equals(tipoPer)) {
      Genero gen = (this.genero != null) ? Genero.valueOf(this.genero.toUpperCase()) : Genero.OTRO;
      Humana humana = new Humana(this.nombre, this.apellido, this.edad, this.numeroDeDocumento, gen, this.nombreAMostrar);
      Donante donante = new Donante(dir, humana);
      donante.getPersona().getMediosDeContacto().agregarMediosDeContacto(medios);
      donante.getPersona().getMediosDeContacto().setMedioDeContactoPredeterminado(medioPred);
      return donante;
    } else if ("JURIDICA".equals(tipoPer)) {
      TipoJuridico tj = (this.tipoJuridico != null) ? TipoJuridico.valueOf(this.tipoJuridico.toUpperCase()) : TipoJuridico.ONG;
      Juridica juridica = new Juridica(this.razonSocial, this.rubro, tj, this.cuit, new ArrayList<>(), this.nombreAMostrar);
      juridica.getMediosDeContacto().agregarMediosDeContacto(medios);
      juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(medioPred);
      Donante donante = new Donante(dir, juridica);

      if (this.representantes != null) {
        List<Representante> reps = this.representantes.stream().map(RepresentanteDTO::toDomain).collect(Collectors.toList());
        ((Juridica) donante.getPersona()).agregarRepresentantes(reps);
      }
      return donante;
    }
    throw new IllegalArgumentException("Tipo de persona inválido");
  }

  public static PersonaDonanteDTO from(Donante entidad) {
    if (entidad == null) return null;
    PersonaDonanteDTO dto = new PersonaDonanteDTO();
    dto.setId(entidad.getId());

    if (entidad.getPersona() != null) {
      dto.setNombreAMostrar(entidad.getPersona().getNombreDeUsuario());
      if (entidad.getPersona().getMediosDeContacto() != null) {
        if(entidad.getPersona().getMediosDeContacto().getListaMediosDeContacto() != null) {
          dto.setMediosDeContacto(entidad.getPersona().getMediosDeContacto().getListaMediosDeContacto().stream().map(MediosContactoDTO::from).collect(Collectors.toList()));
        }
        MedioDeContacto pred = entidad.getPersona().getMediosDeContacto().getMedioDeContactoPredeterminado();
        if (pred != null) dto.setMedioPredeterminado(MediosContactoDTO.from(pred));
      }

      if (entidad.getPersona() instanceof Humana ph) {
        dto.setTipoPersona("HUMANA");
        dto.setNombre(ph.getNombre());
        dto.setApellido(ph.getApellido());
        dto.setEdad(ph.getEdad());
        dto.setNumeroDeDocumento(ph.getNumeroDeDocumento());
        dto.setGenero(ph.getGenero() != null ? ph.getGenero().name() : null);
      } else if (entidad.getPersona() instanceof Juridica pj) {
        dto.setTipoPersona("JURIDICA");
        dto.setRazonSocial(pj.getRazonSocial());
        dto.setCuit(pj.getCuit());
        dto.setRubro(pj.getRubro());
        dto.setTipoJuridico(pj.getTipoJuridico() != null ? pj.getTipoJuridico().name() : null);
        if (pj.getRepresentantes() != null) dto.setRepresentantes(pj.getRepresentantes().stream().map(RepresentanteDTO::from).collect(Collectors.toList()));
      } else {
        dto.setTipoPersona("DESCONOCIDO");
      }
    }
    dto.setDireccion(DireccionDTO.from(entidad.getDireccion()));
    return dto;
  }
}