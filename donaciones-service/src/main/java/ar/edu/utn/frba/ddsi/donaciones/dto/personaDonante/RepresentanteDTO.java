package ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepresentanteDTO {
  private String nombre;
  private String apellido;
  private int numeroDeDocumento;
  private boolean activo;
  private int edad;
  private String genero;

public Representante toDomain() {
    Genero gen = (this.genero != null) ? Genero.valueOf(this.genero.toUpperCase()) : Genero.OTRO;
    Humana humana = new Humana(this.nombre, this.apellido, this.edad, this.numeroDeDocumento, gen);
    return new Representante(humana, this.activo);
}

  public static RepresentanteDTO from(Representante rep) {
    if (rep == null) return null;
    RepresentanteDTO dto = new RepresentanteDTO();
    dto.setActivo(rep.isActivo());
    if (rep.getHumana() != null) {
      dto.setNombre(rep.getHumana().getNombre());
      dto.setApellido(rep.getHumana().getApellido());
      dto.setEdad(rep.getHumana().getEdad());
      dto.setNumeroDeDocumento(rep.getHumana().getNumeroDeDocumento());
      dto.setGenero(rep.getHumana().getGenero() != null ? rep.getHumana().getGenero().name() : null);
    }
    return dto;
  }
}