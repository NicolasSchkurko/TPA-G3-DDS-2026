package ar.edu.utn.frba.ddsi.donaciones.dto;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DireccionDTO {
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;

    public Direccion toDomain() {
        Pais p = new Pais(this.pais != null ? this.pais : "Argentina");
        Provincia prov = new Provincia(this.provincia != null ? this.provincia : "Buenos Aires", p);
        Ciudad c = new Ciudad(this.ciudad != null ? this.ciudad : "CABA", prov);
        return new Direccion(this.calleUno, this.calleDos, this.altura, this.piso, this.departamento, c);
    }

    public static DireccionDTO from(Direccion dir) {
        if (dir == null) return null;
        DireccionDTO dto = new DireccionDTO();
        dto.setCalleUno(dir.getCalleUno());
        dto.setCalleDos(dir.getCalleDos());
        dto.setAltura(dir.getAltura());
        dto.setPiso(dir.getPiso());
        dto.setDepartamento(dir.getDepartamento());
        if (dir.getCiudad() != null) {
            dto.setCiudad(dir.getCiudad().getNombre());
            if (dir.getCiudad().getProvincia() != null) {
                dto.setProvincia(dir.getCiudad().getProvincia().getNombre());
                if (dir.getCiudad().getProvincia().getPais() != null) {
                    dto.setPais(dir.getCiudad().getProvincia().getPais().getNombre());
                }
            }
        }
        return dto;
    }
}