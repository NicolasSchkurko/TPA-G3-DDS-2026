package ar.edu.utn.frba.ddsi.donaciones.entidadbeneficiaria;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntidadBeneficiariaDTOTest {

    @Test
    void mapsEmailAndPhoneToBeneficiaryAndBack() {
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        dto.setRazonSocial("Entidad Ejemplo");
        dto.setTelefono("1122334455");
        dto.setCorreoElectronico("entidad@example.com");

        EntidadBeneficiaria entidad = dto.toDomain(null);
        var medios = entidad.getPersonaJuridica().getMediosDeContacto().getListaMediosDeContacto();

        assertEquals(2, medios.size());
        assertEquals("1122334455", medios.stream()
                .filter(Telefono.class::isInstance)
                .findFirst().orElseThrow().getValor());
        assertEquals("entidad@example.com", medios.stream()
                .filter(Mail.class::isInstance)
                .findFirst().orElseThrow().getValor());

        EntidadBeneficiariaDTO respuesta = EntidadBeneficiariaDTO.from(entidad);
        assertEquals("1122334455", respuesta.getTelefono());
        assertEquals("entidad@example.com", respuesta.getCorreoElectronico());
    }
}
