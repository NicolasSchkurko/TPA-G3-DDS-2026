package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Notificaciones.NotificacionPendiente;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioNotificacionesPendientes;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Outbox de notificaciones: tope de reintentos")
class NotificacionesPendientesServiceTest {

    private final RepositorioNotificacionesPendientes repositorio =
            mock(RepositorioNotificacionesPendientes.class);
    private final NotificacionesPendientesService service =
            new NotificacionesPendientesService(repositorio);

    private NotificacionPendiente pendienteConIntentos(int intentos) {
        NotificacionPendiente pendiente = new NotificacionPendiente(
                new PerfilNotificacionDTO("EMAIL", "ana@example.com", "Mensaje", "Asunto"),
                LocalDateTime.now());
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(pendiente, "id", id);
        for (int i = 0; i < intentos; i++) {
            pendiente.reclamarHasta(LocalDateTime.now().plusSeconds(60));
        }
        when(repositorio.findById(id)).thenReturn(Optional.of(pendiente));
        return pendiente;
    }

    @Test
    @DisplayName("al llegar al máximo de intentos la fila se descarta")
    void alLlegarAlMaximoSeDescarta() {
        NotificacionPendiente pendiente = pendienteConIntentos(10);

        service.reintentar(pendiente.getId(), LocalDateTime.now(), "broker caído");

        assertThat(pendiente.isDescartada()).isTrue();
        assertThat(pendiente.getLeaseHasta()).isNull();
        assertThat(pendiente.getUltimoError()).contains("broker caído");
    }

    @Test
    @DisplayName("antes del máximo se reprograma con backoff")
    void antesDelMaximoSeReprograma() {
        NotificacionPendiente pendiente = pendienteConIntentos(2);
        LocalDateTime ahora = LocalDateTime.now();

        service.reintentar(pendiente.getId(), ahora, "broker caído");

        assertThat(pendiente.isDescartada()).isFalse();
        assertThat(pendiente.getProximoIntento()).isAfter(ahora);
    }
}
