package ar.edu.utn.frba.ddsi.donaciones.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.TipoJuridico;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDePersonas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class RepositorioDePersonasTest {

  private RepositorioDePersonas repositorioPersonas;

  @BeforeEach
  void setUp() {
    repositorioPersonas = mock(RepositorioDePersonas.class, CALLS_REAL_METHODS);
  }

  @Test
  @DisplayName("Punto 41: modificarPersona para Juridica actualiza rubro, tipoJuridico y sincroniza representantes")
  void modificarPersona_juridica_actualizaRubroTipoJuridicoYRepresentantes() {
    UUID personaId = UUID.randomUUID();

    Humana repOriginal = new Humana("Pedro", "Pascal", 45, 11111111, Genero.HOMBRE);
    List<Representante> representantesOriginales = new ArrayList<>();
    representantesOriginales.add(new Representante(repOriginal, true));

    Juridica existente = new Juridica("Tech Corp", "Tecnologia", TipoJuridico.EMPRESA, "30-11111111-9", representantesOriginales);

    Humana nuevoRep = new Humana("Lucia", "Perez", 35, 22222222, Genero.MUJER);
    List<Representante> nuevosRepresentantes = List.of(new Representante(nuevoRep, true));
    Juridica datosNuevos = new Juridica("Tech Corp Nueva", "Biotecnologia", TipoJuridico.ONG, "30-22222222-9", nuevosRepresentantes);

    when(repositorioPersonas.findById(personaId)).thenReturn(Optional.of(existente));
    when(repositorioPersonas.existsById(personaId)).thenReturn(true);

    repositorioPersonas.modificarPersona(personaId, datosNuevos);

    assertEquals("Tech Corp Nueva", existente.getRazonSocial());
    assertEquals("30-22222222-9", existente.getCuit());
    assertEquals("Biotecnologia", existente.getRubro());
    assertEquals(TipoJuridico.ONG, existente.getTipoJuridico());
    assertEquals(1, existente.getRepresentantes().size());
    assertEquals(nuevoRep, existente.getRepresentantes().get(0).getHumana());

    verify(repositorioPersonas).save(existente);
  }
}
