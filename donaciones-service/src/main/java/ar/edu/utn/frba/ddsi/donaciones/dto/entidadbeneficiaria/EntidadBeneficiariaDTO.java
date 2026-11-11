package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.direccion.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
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
    private String rubro;
    private String cuit;
    private String tipoJuridico;
    private String telefono;
    private String correoElectronico;
    private DireccionDTO direccion;
    private UUID id;
    private Boolean activa;

    public EntidadBeneficiaria toDomain(Ciudad ciudad) {
        Juridica juridica = null;
        if (this.razonSocial != null || this.telefono != null) {
            if (this.razonSocial == null || this.razonSocial.isBlank()) {
                throw new IllegalArgumentException("La razón social es obligatoria para registrar una entidad beneficiaria");
            }

            TipoJuridico tipo = this.tipoJuridicoValido()
                    ? TipoJuridico.valueOf(this.tipoJuridico.toUpperCase())
                    : TipoJuridico.ONG;

            juridica = new Juridica(
                    this.razonSocial,
                    this.rubro,
                    tipo,
                    this.cuit,
                    new ArrayList<>()
            );

            if (this.telefono != null && !this.telefono.isEmpty()) {
                Telefono tel = new Telefono(this.telefono);
                juridica.agregarMedioDeContacto(tel);
                juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(tel);
            }
            if (this.correoElectronico != null && !this.correoElectronico.isBlank()) {
                Mail mail = new Mail(this.correoElectronico);
                juridica.agregarMedioDeContacto(mail);
                if (juridica.getMediosDeContacto().getMedioDeContactoPredeterminado() == null) {
                    juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(mail);
                }
            }
        }

        EntidadBeneficiaria entidad = new EntidadBeneficiaria(
                this.direccion != null ? this.direccion.toDomain(ciudad) : null,
                juridica
        );

        if (this.id != null) {
            entidad.setId(this.id);
        }

        return entidad;
    }

    private boolean tipoJuridicoValido() {
        return this.tipoJuridico != null && !this.tipoJuridico.isBlank();
    }

    public static EntidadBeneficiariaDTO from(EntidadBeneficiaria entidad) {
        if (entidad == null) return null;
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();

        if (entidad.getPersonaJuridica() != null) {
            dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());
            dto.setRubro(entidad.getPersonaJuridica().getRubro());
            dto.setCuit(entidad.getPersonaJuridica().getCuit());
            dto.setTipoJuridico(entidad.getPersonaJuridica().getTipoJuridico() != null
                    ? entidad.getPersonaJuridica().getTipoJuridico().name()
                    : null);
            if (entidad.getPersonaJuridica().getMediosDeContacto() != null) {
                MedioDeContacto predeterminado =
                        entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado();
                if (predeterminado instanceof Telefono) {
                    dto.setTelefono(predeterminado.getValor());
                }
                if (entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto() != null) {
                    entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto().forEach(medio -> {
                        if (medio instanceof Telefono && dto.getTelefono() == null) {
                            dto.setTelefono(medio.getValor());
                        } else if (medio instanceof Mail && dto.getCorreoElectronico() == null) {
                            dto.setCorreoElectronico(medio.getValor());
                        }
                    });
                }
            }
        }

        dto.setId(entidad.getId());
        dto.setActiva(entidad.isActiva());
        dto.setDireccion(DireccionDTO.from(entidad.getDireccion()));

        return dto;
    }
}
