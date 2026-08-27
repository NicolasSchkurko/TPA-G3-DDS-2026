package ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.UUID;

@Getter
@Setter
public class EntidadBeneficiariaDTO {
    private String razonSocial;
    private String telefono;
    private DireccionDTO direccion;
    private UUID id;
    public EntidadBeneficiaria toDomain() {
        Juridica juridica = null;
        if (this.razonSocial != null || this.telefono != null) {
            juridica = new Juridica(
                this.razonSocial != null ? this.razonSocial : "ONG Sin Nombre",
                "ONG", TipoJuridico.ONG, "00-00000000-0", new ArrayList<>(),
                this.razonSocial != null ? this.razonSocial : "ONG Sin Nombre"
            );
            if (this.telefono != null && !this.telefono.isEmpty()) {
                Telefono tel = new Telefono(this.telefono);
                juridica.agregarMedioDeContacto(tel);
                juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(tel);
            }
        }
        return new EntidadBeneficiaria(this.direccion != null ? this.direccion.toDomain() : null, juridica);
    }

    public static EntidadBeneficiariaDTO from(EntidadBeneficiaria entidad) {
        if (entidad == null) return null;
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        if (entidad.getPersonaJuridica() != null) {
            dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());
            if (entidad.getPersonaJuridica().getMediosDeContacto() != null && entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado() != null) {
                dto.setTelefono(entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor());
            }
        }
        dto.setId(entidad.getId());
        dto.setDireccion(DireccionDTO.from(entidad.getDireccion()));
        return dto;
    }
}