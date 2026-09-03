package ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class EntidadBeneficiariaDTO {
    private String razonSocial;
    private String telefono;
    private DireccionDTO direccion;
    private UUID id;
    private List<NecesidadDTO> necesidades;

    public EntidadBeneficiaria toDomain(Ciudad ciudad) {
        Juridica juridica = null;
        if (this.razonSocial != null || this.telefono != null) {
            String nombreRS = this.razonSocial != null ? this.razonSocial : "ONG Sin Nombre";

            // Inicializamos Juridica con todos los parámetros requeridos por su constructor actual
            juridica = new Juridica(
                    nombreRS,
                    "ONG",
                    TipoJuridico.ONG,
                    "00-00000000-0",
                    new ArrayList<>()
            );

            if (this.telefono != null && !this.telefono.isEmpty()) {
                Telefono tel = new Telefono(this.telefono);
                juridica.agregarMedioDeContacto(tel);
                juridica.getMediosDeContacto().setMedioDeContactoPredeterminado(tel);
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

    public static EntidadBeneficiariaDTO from(EntidadBeneficiaria entidad) {
        if (entidad == null) return null;
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();

        if (entidad.getPersonaJuridica() != null) {
            dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());
            if (entidad.getPersonaJuridica().getMediosDeContacto() != null &&
                    entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado() != null) {
                dto.setTelefono(entidad.getPersonaJuridica().getMediosDeContacto().getMedioDeContactoPredeterminado().getValor());
            }
        }

        dto.setId(entidad.getId());
        dto.setDireccion(DireccionDTO.from(entidad.getDireccion()));

        // Mapeamos la lista de necesidades asociadas a la entidad
        if (entidad.getNecesidades() != null) {
            dto.setNecesidades(entidad.getNecesidades().stream()
                    .map(NecesidadDTO::from)
                    .collect(Collectors.toList()));
        } else {
            dto.setNecesidades(new ArrayList<>());
        }

        return dto;
    }
}