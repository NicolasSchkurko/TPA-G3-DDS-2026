package ar.edu.utn.frba.ddsi.donaciones.dto.direccion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DireccionDTO {
    /**
     * Id de la EntidadBeneficiaria dueña de esta dirección. Es lo que logística usa para
     * resolver la entidad destino del item de entrega (su EntregaService.resolverEntidad hace
     * findById(idEntidad)); si sale en null, su DonacionListener descarta el mensaje (punto 27).
     */
    private UUID idEntidad;
    private String calleUno;
    private String calleDos;
    private Integer altura;
    private Integer piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;
    private Double latitud;
    private Double longitud;

    public Direccion toDomain() {
        Pais p = new Pais(this.pais != null ? this.pais : "Argentina");
        Provincia prov = new Provincia(this.provincia != null ? this.provincia : "Buenos Aires", p);
        Ciudad c = new Ciudad(this.ciudad != null ? this.ciudad : "CABA", prov);
        return conCoordenadas(new Direccion(this.calleUno, this.calleDos, this.altura, pisoSeguro(), this.departamento, c));
    }

    // Variante que recibe una Ciudad ya persistida/managed (resuelta vía RepositorioDeCiudades,
    // buscar-o-crear) en lugar de construir la cadena Pais/Provincia/Ciudad "al vuelo", que
    // rompía merge() por no estar cascadeada (ver Ciudad/Provincia/Pais sin cascade en Direccion).
    public Direccion toDomain(Ciudad ciudad) {
        return conCoordenadas(new Direccion(this.calleUno, this.calleDos, this.altura, pisoSeguro(), this.departamento, ciudad));
    }

    /** Indica si el request ya trae coordenadas (por ejemplo, elegidas en un mapa). */
    public boolean tieneCoordenadas() {
        return latitud != null && longitud != null;
    }

    private Direccion conCoordenadas(Direccion direccion) {
        direccion.setLatitud(this.latitud);
        direccion.setLongitud(this.longitud);
        return direccion;
    }

    /** El piso es opcional en el request; el dominio lo modela como {@code int}. */
    private int pisoSeguro() {
        return this.piso != null ? this.piso : 0;
    }

    public static DireccionDTO from(Direccion dir) {
        if (dir == null) return null;
        DireccionDTO dto = new DireccionDTO();
        dto.setCalleUno(dir.getCalleUno());
        dto.setCalleDos(dir.getCalleDos());
        dto.setAltura(dir.getAltura());
        dto.setPiso(dir.getPiso());
        dto.setDepartamento(dir.getDepartamento());
        dto.setLatitud(dir.getLatitud());
        dto.setLongitud(dir.getLongitud());
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

    /**
     * Variante sin coordenadas para los datos de donantes que se devuelven al frontend. Las
     * coordenadas de una dirección particular no se publican en los listados; logística las
     * recibe por evento, no por este DTO.
     */
    public static DireccionDTO fromSinCoordenadas(Direccion dir) {
        DireccionDTO dto = from(dir);
        if (dto != null) {
            dto.setLatitud(null);
            dto.setLongitud(null);
        }
        return dto;
    }
}