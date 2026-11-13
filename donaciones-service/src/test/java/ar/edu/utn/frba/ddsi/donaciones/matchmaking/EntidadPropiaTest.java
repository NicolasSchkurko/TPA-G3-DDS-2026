package ar.edu.utn.frba.ddsi.donaciones.matchmaking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.EntidadPropia;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntidadPropia")
class EntidadPropiaTest {

  @Test
  @DisplayName("mismo keycloakSub se considera entidad propia")
  void mismoKeycloakSub() {
    Donante donante = donanteConSub("sub-123", "donante@test.com");
    EntidadBeneficiaria entidad = entidadConSub("sub-123", "entidad@test.com");

    assertTrue(EntidadPropia.esPropia(donante, entidad));
  }

  @Test
  @DisplayName("mismo email se considera entidad propia")
  void mismoEmail() {
    Donante donante = donanteConEmail("mismo@test.com");
    EntidadBeneficiaria entidad = entidadConEmail("mismo@test.com");

    assertTrue(EntidadPropia.esPropia(donante, entidad));
  }

  @Test
  @DisplayName("email de un representante coincide con el del donante")
  void emailRepresentante() {
    Donante donante = donanteConEmail("rep@test.com");

    Humana representanteHumana = new Humana("Rep", "Resentante", 30, 1, Genero.OTRO);
    representanteHumana.getMediosDeContacto().agregarMedioDeContacto(new Mail("rep@test.com"));
    Representante representante = new Representante(representanteHumana, true);

    Juridica juridica = new Juridica("Org", "Rubro", TipoJuridico.ONG, "30-1", new ArrayList<>());
    juridica.agregarRepresentantes(List.of(representante));
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(null, juridica);

    assertTrue(EntidadPropia.esPropia(donante, entidad));
  }

  @Test
  @DisplayName("entidad distinta está permitida")
  void entidadDistinta() {
    Donante donante = donanteConSub("sub-donante", "donante@test.com");
    EntidadBeneficiaria entidad = entidadConSub("sub-entidad", "entidad@test.com");

    assertFalse(EntidadPropia.esPropia(donante, entidad));
  }


  @Test
  @DisplayName("el email de un representante del donante jurídico también identifica la entidad propia")
  void emailRepresentanteDelDonanteJuridico() {
    Humana representanteDonante = new Humana("Ana", "Perez", 30, 1, Genero.OTRO);
    representanteDonante.getMediosDeContacto().agregarMedioDeContacto(new Mail("representante@test.com"));
    Juridica juridicaDonante = new Juridica("Donante SA", "Rubro", TipoJuridico.EMPRESA, "30-2", new ArrayList<>());
    juridicaDonante.agregarRepresentantes(List.of(new Representante(representanteDonante, true)));
    Donante donante = new Donante(null, juridicaDonante);

    EntidadBeneficiaria entidad = entidadConEmail("representante@test.com");

    assertTrue(EntidadPropia.esPropia(donante, entidad));
  }

  private static Donante donanteConSub(String sub, String email) {
    Humana humana = new Humana("Ana", "Perez", 30, 1, Genero.OTRO);
    humana.getMediosDeContacto().agregarMedioDeContacto(new Mail(email));
    Donante donante = new Donante(null, humana);
    donante.setKeycloakSub(sub);
    return donante;
  }

  private static Donante donanteConEmail(String email) {
    Humana humana = new Humana("Ana", "Perez", 30, 1, Genero.OTRO);
    humana.getMediosDeContacto().agregarMedioDeContacto(new Mail(email));
    return new Donante(null, humana);
  }

  private static EntidadBeneficiaria entidadConSub(String sub, String email) {
    Juridica juridica = new Juridica("Org", "Rubro", TipoJuridico.ONG, "30-1", new ArrayList<>());
    juridica.getMediosDeContacto().agregarMedioDeContacto(new Mail(email));
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(null, juridica);
    entidad.setKeycloakSub(sub);
    return entidad;
  }

  private static EntidadBeneficiaria entidadConEmail(String email) {
    Juridica juridica = new Juridica("Org", "Rubro", TipoJuridico.ONG, "30-1", new ArrayList<>());
    juridica.getMediosDeContacto().agregarMedioDeContacto(new Mail(email));
    return new EntidadBeneficiaria(null, juridica);
  }
}
