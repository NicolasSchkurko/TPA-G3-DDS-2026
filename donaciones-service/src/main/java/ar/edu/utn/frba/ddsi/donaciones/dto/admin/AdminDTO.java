package ar.edu.utn.frba.ddsi.donaciones.dto.admin;

import ar.edu.utn.frba.ddsi.donaciones.dto.notificaciones.MediosContactoDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminDTO {
    private String id;
    private String nombreAMostrar;
    private String nombre;
    private String apellido;
    private int edad;
    private int numeroDeDocumento;
    private String genero;
    private MediosContactoDTO medioDeContacto;
    private String email;

    public Administrador toDomain() {
        Genero gen = (this.genero != null) ? Genero.valueOf(this.genero.toUpperCase()) : Genero.OTRO;
        MedioDeContacto contacto = (this.medioDeContacto != null) ? this.medioDeContacto.toDomain() : null;
        if (contacto == null) throw new IllegalArgumentException("El administrador debe tener un medio de contacto asignado.");
        if (this.email != null && !this.email.isBlank() && !this.email.contains("@")) {
            throw new IllegalArgumentException("Ingresá un correo electrónico válido");
        }
        Humana humano = new Humana(this.nombre, this.apellido, this.edad, this.numeroDeDocumento, gen);
        if (this.email != null && !this.email.isBlank()) {
            Mail mail = new Mail(this.email);
            humano.agregarMedioDeContacto(mail);
            humano.getMediosDeContacto().setMedioDeContactoPredeterminado(mail);
        }
        return new Administrador(humano, contacto, this.nombreAMostrar);
    }

    public static AdminDTO from(Administrador admin) {
        if (admin == null) return null;
        AdminDTO dto = new AdminDTO();
        dto.setId(String.valueOf(admin.getId()));
        dto.setNombreAMostrar(admin.getNombreAMostrar());

        if (admin.getHumano() != null) {
            dto.setNombre(admin.getHumano().getNombre());
            dto.setApellido(admin.getHumano().getApellido());
            dto.setEdad(admin.getHumano().getEdad());
            dto.setNumeroDeDocumento(admin.getHumano().getNumeroDeDocumento());
            dto.setGenero(admin.getHumano().getGenero() != null ? admin.getHumano().getGenero().name() : null);
            if (admin.getHumano().getMediosDeContacto() != null
                    && admin.getHumano().getMediosDeContacto().getListaMediosDeContacto() != null) {
                for (MedioDeContacto medio : admin.getHumano().getMediosDeContacto().getListaMediosDeContacto()) {
                    if (medio instanceof Mail mail) {
                        dto.setEmail(mail.getDireccionMail());
                        break;
                    }
                }
            }
        }
        if (admin.getContacto() != null) {
            dto.setMedioDeContacto(MediosContactoDTO.from(admin.getContacto()));
            if (dto.getEmail() == null && admin.getContacto() instanceof Mail mail) {
                dto.setEmail(mail.getDireccionMail());
            }
        }
        return dto;
    }
}