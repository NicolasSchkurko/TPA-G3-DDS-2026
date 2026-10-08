package ar.edu.utn.frba.ddsi.incentivos.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import ar.edu.utn.frba.ddsi.incentivos.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@DisplayName("NotificacionClient: publicacion en el broker de notificaciones")
class NotificacionClientTest {

    private RabbitTemplate rabbitTemplate;
    private RepositorioNotificacionesPendientes pendientes;
    private NotificacionClient client;

    @BeforeEach
    void setUp() {
        rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
        pendientes = new RepositorioNotificacionesPendientes();
        client = new NotificacionClient(rabbitTemplate, null, pendientes);
    }

    private PerfilNotificacionDTO notificacion() {
        return new PerfilNotificacionDTO("EMAIL", "ana@example.com", "Mensaje", "Asunto");
    }

    @Test
    @DisplayName("publica en el exchange de notificaciones con la routing key de incentivo")
    void publicaEnElExchange() {
        client.enviarNotificacion(notificacion());

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NOTIFICACIONES),
                eq(RabbitMQConfig.RK_INCENTIVO),
                any(PerfilNotificacionDTO.class));

        assertThat(pendientes.listarTodas()).isEmpty();
    }

    /** El nombre del campo es contrato con el receptor. */
    @Test
    @DisplayName("el DTO lleva direccionDeContacto, el nombre que lee el receptor")
    void elCampoDeDireccionSeLlamaComoLoEsperaElReceptor() {
        client.enviarNotificacion(notificacion());

        ArgumentCaptor<PerfilNotificacionDTO> captor =
                ArgumentCaptor.forClass(PerfilNotificacionDTO.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NOTIFICACIONES),
                eq(RabbitMQConfig.RK_INCENTIVO),
                captor.capture());

        assertThat(captor.getValue().getDireccionDeContacto()).isEqualTo("ana@example.com");
    }

    @Test
    @DisplayName("si el broker falla, guarda la notificacion como pendiente y propaga el error")
    void guardaLaNotificacionPendienteSiElBrokerFalla() {
        // Los tipos explicitos evitan la ambiguedad de convertAndSend, que tiene varias
        // sobrecargas y Mockito no puede desambiguar entre matchers genericos.
        doThrow(new AmqpConnectException(new java.io.IOException("broker caido")))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(PerfilNotificacionDTO.class));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> client.enviarNotificacion(notificacion()))
                .isInstanceOf(EnvioNotificacionException.class);

        assertThat(pendientes.listarTodas()).hasSize(1);
    }
}
