package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Como decide el listener frente a las carreras y los fallos que aparecen con N instancias.
 *
 * <p>La distincion importa: <b>no todas las excepciones son lo mismo</b>, y mandarlas todas a la
 * dead letter queue seria contraproducente, porque la cola es compartida y un mensaje trabado
 * frena a todas las instancias a la vez.
 *
 * <p><b>Los reintentos no se prueban durmiendo.</b> El listener recibe la espera por parametro, y
 * aca se pasa cero: lo que se verifica es la cantidad de intentos y que el error escape o no,
 * que es lo que decide si el mensaje va a la DLQ o se descarta.
 */
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
        // Dos instancias leen el mismo idDonacion antes de que ninguna lo escriba. Ambas pasan
        // el existsById, las dos intentan insertar. Gana una; la otra recibe la violacion de clave
        // primaria, que es exactamente el estado final que se buscaba. Mandarla a la DLQ seria
        // reportar como fallo haber conseguido el resultado correcto, y ademas trabaria la cola
        // compartida para el resto de las instancias.
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

    /**
     * El mensaje tiene que salir del listener como un rechazo SIN reencolar.
     *
     * <p><b>Por que importa el tipo de la excepcion y no solo que salga alguna.</b> Spring AMQP
     * reencola por defecto todo lo que sale del listener, asi que un {@code throw ultimoFallo} a
     * secas devuelve el mensaje a su posicion original en la cola compartida y vuelve a entrar:
     * el mismo payload falla tres veces mas, para siempre. Con la cola compartida de la que
     * dependen todas las instancias, un unico mensaje malformado frena las notificaciones de
     * toda la base, y la dead letter queda vacia porque nunca se activo nada.
     *
     * <p>Por eso la causa original se conserva: el operador que mira la DLQ tiene que ver por que
     * fallo el mensaje, no solo que se descarto.
     */
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
    @DisplayName("Un mensaje que se procesa bien se procesa una sola vez")
    void mensajeSanoSeProcesaUnaVez() {
        EntregaDTO dto = peticion();

        assertThatCode(() -> listener(INTENTOS).recibirDonacionParaEntregar(dto))
                .doesNotThrowAnyException();

        verify(entregaService, times(1)).procesarPeticion(dto);
    }
}