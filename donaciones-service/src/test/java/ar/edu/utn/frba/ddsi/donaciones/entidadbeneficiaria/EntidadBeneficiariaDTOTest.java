// El paquete tiene que coincidir EXACTAMENTE con el directorio (entidadBeneficiaria, con B
// mayúscula), igual que el EntidadBeneficiariaServiceTest de al lado. Antes decía
// "entidadbeneficiaria" todo en minúscula: en Windows compilaba igual porque el filesystem no
// distingue mayúsculas y el .class terminaba en el directorio con B mayúscula, pero Surefire
// deduce el nombre de la clase del directorio y el JVM rechazaba cargarla
// ("wrong name"), lo que hacía fallar TODO el módulo sin correr un solo test en Linux/CI.
package ar.edu.utn.frba.ddsi.donaciones.entidadBeneficiaria;

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
