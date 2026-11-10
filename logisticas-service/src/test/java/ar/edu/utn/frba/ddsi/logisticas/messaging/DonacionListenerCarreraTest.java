package ar.edu.utn.frba.ddsi.logisticas.messaging;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienesDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonacionListener reintenta lo transitorio y descarta lo que no tiene arreglo")
class DonacionListenerCarreraTest {

    private static final int INTENTOS = 3;

    @Mock private EntregaService entregaService;

    private static EntregaDTO peticion() {
        DireccionDTO direccion = new DireccionDTO(UUID.randomUUID(),
                "Calle", "100", 100, 1, "A", "CABA", "Buenos Aires", "Argentina");

        EntregaDTO dto = new EntregaDTO(List.of(UUID.randomUUID()),
                List.of(new BienDTO(1, "KILOGRAMOS", null, null, null, null, null)), direccion);
        return dto;
    }

    private DonacionListener listener(int intentos) {
        return new DonacionListener(entregaService, intentos, 0);
    }

    @Test
    @DisplayName("Una violacion de clave primaria se traga: otra instancia ya la registro")
    void violacionDeClavePrimariaNoEsUnFallo() {
        EntregaDTO dto = peticion();
        doThrow(new DataIntegrityViolationException("Duplicate entry for key 'PRIMARY'"))
                .when(entregaService).procesarPeticion(dto);

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("La carrera de clave primaria no gasta los otros intentos")
    void laCarreraNoGastaLosReintentos() {
        // DataIntegrityViolationException es subclase de IllegalStateException, que si se
        // reintentaria. Sin su propio catch, un resultado correcto se gastaria los tres intentos
        // multiplicando por tres los mensajes que traban la cola compartida.
        EntregaDTO dto = peticion();
        doThrow(new DataIntegrityViolationException("Duplicate entry"))
                .when(entregaService).procesarPeticion(dto);

        listener(INTENTOS).recibirDonacionParaEntregar(dto);

        verify(entregaService, times(1)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Un error de negocio tampoco va a la DLQ: va a fallar igual en cada intento")
    void errorDeNegocioNoSeRelanza() {
        EntregaDTO dto = peticion();
        doThrow(new IllegalArgumentException("La unidad de medida no es soportada: bananas"))
                .when(entregaService).procesarPeticion(dto);

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .doesNotThrowAnyException();

        verify(entregaService, times(1)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Un fallo transitorio se reintenta y al segundo intento sale")
    void falloTransitorioSeRecupera() {
        // La base tardo, se corto la conexion, el pool estaba ocupado. El mensaje no se proceso de
        // verdad, asi que reintentar es lo correcto: al segundo intento la base responde y el
        // mensaje se procesa sin que nadie se entere.
        EntregaDTO dto = peticion();
        doThrow(new OptimisticLockingFailureException("Row was updated or deleted"))
                .doNothing()
                .when(entregaService).procesarPeticion(dto);

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .doesNotThrowAnyException();

        verify(entregaService, times(2)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Un fallo que no se arregla se intenta las veces configuradas y recien ahi va a la DLQ")
    void falloPersistenteAgaotaLosIntentos() {
        EntregaDTO dto = peticion();
        doThrow(new IllegalStateException("La base no responde"))
                .when(entregaService).procesarPeticion(dto);

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(entregaService, times(INTENTOS)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Agotados los intentos el mensaje se rechaza sin reencolar, no vuelve a la cola")
    void falloPersistenteRechazaSinReencolar() {
        EntregaDTO dto = peticion();
        doThrow(new IllegalStateException("La base no responde"))
                .when(entregaService).procesarPeticion(dto);

        assertThatThrownBy(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Con un solo intento, un fallo transitorio va derecho a la DLQ")
    void conUnSoloIntentoNoHayReintento() {
        // El reintento tiene que ser configurable de verdad: si no, no se puede bajar a uno
        // cuando se prefiera fallar rapido y que otro servicio lo reintente.
        EntregaDTO dto = peticion();
        doThrow(new IllegalStateException("La base no responde"))
                .when(entregaService).procesarPeticion(dto);

        assertThatCode(() -> listener(1).recibirDonacionParaEntregar(dto))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);

        verify(entregaService, times(1)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Un fallo transitorio que se recupera en el ultimo intento no va a la DLQ")
    void seRecuperaEnElUltimoIntento() {
        // El borde: si solo un intento mas de los tres y el mensaje se procesa, el resultado es
        // el correcto y no debe reportarse como fallo. Un `intento < intentos` mal puesto
        // cortaria el bucle antes del ultimo.
        EntregaDTO dto = peticion();
        doThrow(new IllegalStateException("La base no responde"))
                .doThrow(new IllegalStateException("La base no responde"))
                .doNothing()
                .when(entregaService).procesarPeticion(dto);

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .doesNotThrowAnyException();

        verify(entregaService, times(INTENTOS)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Si interrumpen la espera entre reintentos, no sigue reintentando")
    void interrupcionAbortaLosReintentos() throws Exception {
        // El apagado del contexto interrumpe el hilo. Antes se restablecía el flag pero el bucle
        // seguía: el mensaje se reintentaba contra un servicio que ya se estaba cerrando.
        EntregaDTO dto = peticion();
        CountDownLatch primerIntento = new CountDownLatch(1);
        doAnswer(invocacion -> {
            primerIntento.countDown();
            throw new IllegalStateException("La base no responde");
        }).when(entregaService).procesarPeticion(dto);

        DonacionListener listener = new DonacionListener(entregaService, INTENTOS, 10_000);
        AtomicReference<Throwable> salida = new AtomicReference<>();
        Thread hilo = new Thread(() -> {
            try {
                listener.recibirDonacionParaEntregar(dto);
            } catch (Throwable error) {
                salida.set(error);
            }
        });
        hilo.setDaemon(true);

        hilo.start();
        assertThat(primerIntento.await(5, TimeUnit.SECONDS)).isTrue();
        hilo.interrupt();
        hilo.join(5_000);

        assertThat(hilo.isAlive()).isFalse();
        assertThat(salida.get()).isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verify(entregaService, times(1)).procesarPeticion(dto);
    }

    @Test
    @DisplayName("Un mensaje que se procesa bien se procesa una sola vez")
    void mensajeSanoSeProcesaUnaVez() {
        EntregaDTO dto = peticion();

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .doesNotThrowAnyException();

        verify(entregaService, times(1)).procesarPeticion(dto);
    }
}