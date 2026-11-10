package ar.edu.utn.frba.ddsi.donaciones.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Genero;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.administrador.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces.RepositorioDeAdministradores;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("RepositorioDeAdministradores")
class RepositorioDeAdministradoresTest {

  @Test
  @DisplayName("actualizar fuerza el ID original antes de save")
  void actualizarAsignaIdOriginalAntesDeGuardar() {
    RepositorioDeAdministradores repo = mock(RepositorioDeAdministradores.class, CALLS_REAL_METHODS);

    UUID idOriginal = UUID.randomUUID();
    when(repo.existsById(idOriginal)).thenReturn(true);

    Administrador nuevoObjetoConOtroId = new Administrador(
        new Humana("Test", "Admin", 40, 12345678, Genero.OTRO),
        new Whatsapp("11223344"),
        "Test Admin"
    );

    repo.actualizar(idOriginal, nuevoObjetoConOtroId);

    ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
    verify(repo, times(1)).save(captor.capture());

    assertEquals(idOriginal, captor.getValue().getId(),
        "Debe reasignar el idOriginal antes de save para que Hibernate haga UPDATE y no INSERT");
  }
}
