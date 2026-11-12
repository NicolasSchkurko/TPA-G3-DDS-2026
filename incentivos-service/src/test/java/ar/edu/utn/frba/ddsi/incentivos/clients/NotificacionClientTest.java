package ar.edu.utn.frba.ddsi.incentivos.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.incentivos.dto.Notificaciones.PerfilNotificacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.EnvioNotificacionException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.services.NotificacionesPendientesService.NotificacionReclamada;
import ar.edu.utn.frba.ddsi.incentivos.services.NotificacionesPendientesService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@DisplayName("NotificacionClient")
class NotificacionClientTest {
        @Nested
        @DisplayName("NotificacionClient: publicacion en el broker de notificaciones")
        class PublicacionEnBroker {


        private RabbitTemplate rabbitTemplate;
        private NotificacionesPendientesService pendientes;
        private NotificacionClient client;

        @BeforeEach
        void setUp() {
            rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
            pendientes = org.mockito.Mockito.mock(NotificacionesPendientesService.class);
            client = new NotificacionClient(rabbitTemplate, null, pendientes, new SyncTaskExecutor());
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

            verify(pendientes, never()).encolar(any());
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

            verify(pendientes).encolar(any(PerfilNotificacionDTO.class));
        }

        @Test
        @DisplayName("el scheduler confirma la notificación cuando el broker la acepta")
        void elSchedulerConfirmaLaNotificacion() {
            UUID id = UUID.randomUUID();
            PerfilNotificacionDTO dto = notificacion();
            when(pendientes.reclamarSiguiente(any()))
                    .thenReturn(Optional.of(new NotificacionReclamada(id, dto, 1)))
                    .thenReturn(Optional.empty());

            client.procesarNotificacionesPendientes();

            verify(rabbitTemplate).convertAndSend(
                    eq(RabbitMQConfig.EXCHANGE_NOTIFICACIONES),
                    eq(RabbitMQConfig.RK_INCENTIVO),
                    eq(dto));
            verify(pendientes).confirmar(id);
        }

        @Test
        @DisplayName("el scheduler reprograma la notificación cuando el broker falla")
        void elSchedulerReintentaLaNotificacion() {
            UUID id = UUID.randomUUID();
            PerfilNotificacionDTO dto = notificacion();
            when(pendientes.reclamarSiguiente(any()))
                    .thenReturn(Optional.of(new NotificacionReclamada(id, dto, 2)))
                    .thenReturn(Optional.empty());
            doThrow(new AmqpConnectException(new java.io.IOException("broker caido")))
                    .when(rabbitTemplate)
                    .convertAndSend(anyString(), anyString(), any(PerfilNotificacionDTO.class));

            client.procesarNotificacionesPendientes();

            verify(pendientes).reintentar(eq(id), any(), anyString());
            verify(pendientes, never()).confirmar(id);
        }
        }

        @Nested
        @DisplayName("NotificacionClient: el contacto se resuelve en el listener")
        class ResolucionDeContacto {


        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final UUID USUARIO = UUID.randomUUID();

        private DonacionClient donacionClient;
        private RabbitTemplate rabbitTemplate;
        private NotificacionClient client;

        @BeforeEach
        void setUp() {
            // Mocks por test: si fueran estaticos, las invocaciones de un test contaminarian
            // las verificaciones del siguiente.
            donacionClient = mock(DonacionClient.class);
            rabbitTemplate = mock(RabbitTemplate.class);
            client = new NotificacionClient(
                    rabbitTemplate,
                    donacionClient,
                    mock(NotificacionesPendientesService.class),
                    new SyncTaskExecutor());
        }

        private static MisionCambiada evento() {
            return new MisionCambiada("Racha", "Constancia solidaria", "Ana", USUARIO, "Completitud");
        }

        @Test
        @DisplayName("el evento lleva el id del donante, no el contacto")
        void elEventoLlevaElIdYNoElContacto() {
            // El record ya no tiene campo de contacto: el listener tiene que poder consultarlo.
            assertThat(evento().idUsuario()).isEqualTo(USUARIO);
        }

        @Test
        @DisplayName("el listener busca el contacto antes de notificar")
        void elListenerBuscaElContactoAntesDeNotificar() {
            when(donacionClient.obtenerContactoPersona(USUARIO))
                    .thenReturn(new MedioContacto("discord", "ana#1234"));

            client.notificarCambioMision(evento());

            // La resolucion ocurre en el listener, que corre en AFTER_COMMIT y por lo tanto ya
            // esta fuera de la transaccion. Antes ocurria al armar el evento, adentro.
            verify(donacionClient).obtenerContactoPersona(USUARIO);
        }

        @Test
        @DisplayName("el listener delega el HTTP al executor y no bloquea al que commitea")
        void elListenerNoBloqueaConElHttp() {
            TaskExecutor executor = mock(TaskExecutor.class);
            DonacionClient donacion = mock(DonacionClient.class);
            NotificacionClient asyncClient = new NotificacionClient(
                    mock(RabbitTemplate.class),
                    donacion,
                    mock(NotificacionesPendientesService.class),
                    executor);

            asyncClient.notificarCambioMision(evento());

            verifyNoInteractions(donacion);
            ArgumentCaptor<Runnable> tarea = ArgumentCaptor.forClass(Runnable.class);
            verify(executor).execute(tarea.capture());

            when(donacion.obtenerContactoPersona(USUARIO))
                    .thenReturn(new MedioContacto("discord", "ana#1234"));
            tarea.getValue().run();

            verify(donacion).obtenerContactoPersona(USUARIO);
        }

        @Test
        @DisplayName("sin contacto no se intenta notificar")
        void sinContactoNoSeIntentaNotificar() {
            when(donacionClient.obtenerContactoPersona(USUARIO)).thenReturn(null);

            client.notificarCambioMision(evento());

            // verifyNoInteractions: convertAndSend tiene sobrecargas y Mockito no puede desambiguar.
            verifyNoInteractions(rabbitTemplate);
        }

        @Test
        @DisplayName("cambiar de misión dentro del agregado no toca la red")
        void cambiarDeMisionEnElAgregadoNoTocaLaRed() {
            // La parte estructural del arreglo: Perfil.cambiarMision ya no recibe un contacto,
            // asi que no hay por donde meter una llamada HTTP dentro de la transaccion.
            Mision racha = new Mision("Racha", null, "3 meses", "Constancia",
                    new Regla(null, AtributoImpacto.ESTADO,
                            new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"))));
            Mision completitud = new Mision("Completitud", null, "3 categorias", "Completo",
                    new Regla(null, AtributoImpacto.ESTADO,
                            new CantidadCoincidencias(1, MAPPER.valueToTree("ENTREGADA"))));

            Perfil perfil = new Perfil(USUARIO, "Ana");
            perfil.cambiarMision(racha, null);

            perfil.cambiarMision(completitud, racha);

            assertThat(perfil.getProgresoMisionActual().getMision()).isEqualTo(completitud);
            verify(donacionClient, never()).obtenerContactoPersona(USUARIO);
        }
        }

}
