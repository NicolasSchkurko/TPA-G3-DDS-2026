package ar.edu.utn.frba.ddsi.incentivos.clients;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mensaje.MedioContacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.events.MisionCambiada;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioNotificacionesPendientes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * El contacto del donante ya no viaja resuelto en el evento: lo resuelve el listener
 * (punto 12).
 */
@DisplayName("NotificacionClient: el contacto se resuelve en el listener")
class NotificacionClientResuelveContactoTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final UUID USUARIO = UUID.randomUUID();

    private DonacionClient donacionClient;
    private RestTemplate restTemplate;
    private NotificacionClient client;

    @BeforeEach
    void setUp() {
        // Mocks por test: si fueran estaticos, las invocaciones de un test contaminarian
        // las verificaciones del siguiente.
        donacionClient = mock(DonacionClient.class);
        restTemplate = mock(RestTemplate.class);
        client = new NotificacionClient(
                restTemplate,
                donacionClient,
                mock(RepositorioNotificacionesPendientes.class));
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
    @DisplayName("sin contacto no se intenta notificar")
    void sinContactoNoSeIntentaNotificar() {
        when(donacionClient.obtenerContactoPersona(USUARIO)).thenReturn(null);

        client.notificarCambioMision(evento());

        // verifyNoInteractions y no un verify(restTemplate, never()).postForEntity(...):
        // ese metodo tiene dos sobrecargas compatibles entre si y Mockito no puede
        // desambiguar, con lo que la verificacion pasaria sin comprobar nada.
        verifyNoInteractions(restTemplate);
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
        perfil.setProgresoMisionActual(new ProgresoMision(racha));

        perfil.cambiarMision(completitud, racha);

        assertThat(perfil.getProgresoMisionActual().getMision()).isEqualTo(completitud);
        verify(donacionClient, never()).obtenerContactoPersona(USUARIO);
    }
}
