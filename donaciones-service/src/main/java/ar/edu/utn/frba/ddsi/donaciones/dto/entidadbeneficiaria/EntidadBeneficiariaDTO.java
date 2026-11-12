package ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.dto.direccion.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
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
    /** Email canónico de la entidad. */
    private String email;
    /**
     * Alias de compatibilidad con el frontend/formularios de Sofi: apunta al mismo
     * Mail que {@link #email}. Si se envía solo este, se mapea al Mail de la entidad.
     */
    private String correoElectronico;
    private DireccionDTO direccion;
    private UUID id;
    private Boolean activa;

    public EntidadBeneficiaria toDomain(Ciudad ciudad) {
        Juridica juridica = null;
        String mail = emailValido() ? email : (correoElectronico != null && !correoElectronico.isBlank() ? correoElectronico : null);
        if (this.razonSocial != null || this.telefono != null || mail != null) {
            if (this.razonSocial == null || this.razonSocial.isBlank()) {
                throw new IllegalArgumentException("La razón social es obligatoria para registrar una entidad beneficiaria");
            }
            if (mail != null && !mail.contains("@")) {
                throw new IllegalArgumentException("Ingresá un correo electrónico válido");
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

            Telefono tel = null;
            if (this.telefono != null && !this.telefono.isEmpty()) {
                tel = new Telefono(this.telefono);
                juridica.agregarMedioDeContacto(tel);
            }

            Mail mailContacto = null;
            if (mail != null) {
                mailContacto = new Mail(mail);
                juridica.agregarMedioDeContacto(mailContacto);
            }

            if (tel != null) {
                juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(tel);
            } else if (mailContacto != null) {
                juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(mailContacto);
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

    private boolean emailValido() {
        return this.email != null && !this.email.isBlank();
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
            if (entidad.getPersonaJuridica().getMediosDeContacto() != null
                    && entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto() != null) {
                for (MedioDeContacto medio : entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto()) {
                    if (medio instanceof Mail mail) {
                        // Unificamos: ambos campos exponen el mismo Mail canónico.
                        dto.setEmail(mail.getDireccionMail());
                        dto.setCorreoElectronico(mail.getDireccionMail());
                    } else if (medio instanceof Telefono telefono) {
                        dto.setTelefono(telefono.getValor());
                    }
                }
            }
        }

        dto.setId(entidad.getId());
        dto.setActiva(entidad.isActiva());
        dto.setDireccion(DireccionDTO.from(entidad.getDireccion()));

        return dto;
    }
}
