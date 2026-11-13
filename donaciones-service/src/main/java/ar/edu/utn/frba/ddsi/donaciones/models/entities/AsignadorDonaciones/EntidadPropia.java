package ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MediosDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Política que determina si una entidad beneficiaria pertenece al mismo usuario que el donante.
 * Se usa para impedir que una donación se asigne a la entidad propia.
 */
public final class EntidadPropia {

    private EntidadPropia() {
    }

    public static boolean esPropia(Donante donante, EntidadBeneficiaria entidad) {
        if (donante == null || entidad == null) {
            return false;
        }

        String subDonante = normalizar(donante.getKeycloakSub());
        String subEntidad = normalizar(entidad.getKeycloakSub());
        if (!subDonante.isEmpty() && subDonante.equals(subEntidad)) {
            return true;
        }

        Set<String> emailsDonante = emailsDonante(donante);
        if (emailsDonante.isEmpty()) {
            return false;
        }
        return emailsEntidad(entidad).stream().anyMatch(emailsDonante::contains);
    }

    public static boolean esPropia(Donacion donacion, EntidadBeneficiaria entidad) {
        return esPropia(donacion == null ? null : donacion.getDonante(), entidad);
    }

    private static Set<String> emailsDonante(Donante donante) {
        Set<String> emails = new HashSet<>();
        Persona persona = donante.getPersona();
        if (persona == null) {
            return emails;
        }
        emails.addAll(emailsDePersona(persona));
        if (persona instanceof Juridica juridica && juridica.getRepresentantes() != null) {
            for (Representante representante : juridica.getRepresentantes()) {
                if (representante != null && representante.getHumana() != null) {
                    emails.addAll(emailsDePersona(representante.getHumana()));
                }
            }
        }
        return emails;
    }

    private static Set<String> emailsEntidad(EntidadBeneficiaria entidad) {
        Set<String> emails = new HashSet<>();
        Juridica juridica = entidad.getPersonaJuridica();
        if (juridica == null) {
            return emails;
        }
        emails.addAll(emailsDePersona(juridica));
        List<Representante> representantes = juridica.getRepresentantes();
        if (representantes != null) {
            for (Representante representante : representantes) {
                if (representante != null && representante.getHumana() != null) {
                    emails.addAll(emailsDePersona(representante.getHumana()));
                }
            }
        }
        return emails;
    }

    private static Set<String> emailsDePersona(Persona persona) {
        Set<String> emails = new HashSet<>();
        MediosDeContacto medios = persona.getMediosDeContacto();
        if (medios == null || medios.getListaMediosDeContacto() == null) {
            return emails;
        }
        for (MedioDeContacto medio : medios.getListaMediosDeContacto()) {
            if (medio instanceof Mail mail) {
                String email = normalizar(mail.getDireccionMail());
                if (!email.isEmpty()) {
                    emails.add(email);
                }
            }
        }
        return emails;
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase();
    }
}
