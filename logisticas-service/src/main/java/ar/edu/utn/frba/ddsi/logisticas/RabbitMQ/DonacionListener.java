package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Recibe las donaciones que esperan ser entregadas y las registra como ítems de entrega.
 *
 * <p><b>Por defecto todas las instancias escuchan la misma cola.</b> El SpEL de
 * {@code @RabbitListener} resuelve el bean {@code colasDeEstaInstancia}, que devuelve la cola
 * única salvo que esté configurado el reparto por shards. Con una cola compartida, si una
 * instancia se cae las demás siguen consumiendo y no se traba nada.
 *
 * <p><b>Reintenta antes de rendirse.</b> Un fallo de infraestructura (la base tardó, se cortó la
 * conexión) no va derecho a la cola de mensajes muertos: se reintenta con espera. Reintentar
 * convierte un fallo duro en uno blando, y a la vez cubre el caso de que aparezca un mensaje con
 * una transición de estado y llegue un poco antes que el que la habilita.
 *
 * <p><b>Los errores de negocio no se reintentan.</b> Un payload inválido va a fallar igual en
 * cada intento, y reintentarlo solo|frena la cola compartida de la que salen todas las
 * instancias.
 *
 * <p><b>Una violación de clave primaria tampoco.</b> Es la carrera entre dos instancias que leen
 * el mismo {@code idDonacion} antes de que ninguna lo escriba: gana una y la otra recibe el
 * error, que es el estado final buscado. Va en su propio {@code catch} porque es
 * {@code IllegalStateException}, que sí es de las que se reintentan, y sin ese {@code catch}
 * un resultado correcto se gastaría los tres intentos.
 */
@Component
public class DonacionListener {

    private static final Logger log = LoggerFactory.getLogger(DonacionListener.class);

    private final EntregaService entregaService;
    private final int intentos;
    private final long esperaMs;

    public DonacionListener(
            EntregaService entregaService,
            @Value("${logistica.reintentos:3}") int intentos,
            @Value("${logistica.espera-reintento-ms:2000}") long esperaMs) {
        this.entregaService = entregaService;
        this.intentos = intentos;
        this.esperaMs = esperaMs;
    }

    @RabbitListener(queues = "#{colasDeEstaInstancia}")
    public void recibirDonacionParaEntregar(EntregaDTO peticion) {
        RuntimeException ultimoFallo = null;

        for (int intento = 1; intento <= intentos; intento++) {
            try {
                entregaService.procesarPeticion(peticion);
                return;
            } catch (IllegalArgumentException errorDeNegocio) {
                // Un payload invalido falla igual las veces que lo intentes: se descarta y se
                // sigue, sin gastar mas intentos ni frenar la cola.
                log.warn("Donación descartada por datos inválidos: {}", errorDeNegocio.getMessage());
                return;
            } catch (DataIntegrityViolationException yaRegistrada) {
                // La carrera benigna entre dos instancias: gana una y la otra recibe el error,
                // que es justo el estado final buscado. Relanzarlo mandaria el mensaje a la
                // dead letter queue como si fuera un fallo, y bloquearia la cola compartida de
                // la que salen todas las instancias.
                log.info("La donación ya fue registrada por otra instancia, mensaje descartado "
                        + "sin ir a la cola de mensajes muertos");
                return;
            } catch (RuntimeException error) {
                ultimoFallo = error;
                log.warn("Falló el procesamiento de una donación (intento {} de {}): {}",
                        intento, intentos, error.getMessage());
                if (intento < intentos) {
                    dormir();
                }
            }
        }

        log.error("La donación no se pudo procesar en {} intentos, va a la cola de mensajes "
                + "muertos", intentos, ultimoFallo);
        throw ultimoFallo;
    }

    /**
     * Espera entre intentos.
     *
     * <p><b>Duerme el hilo del listener, y es a proposito.</b> La alternativa, tirar el mensaje y
     * que otro lo tome, no sirve: el mensaje ya se consumio de la cola y nadie mas lo va a leer.
     *
     * <p><b>Si la espera falla, se sigue igual.</b> {@code InterruptedException} no es un fallo de
     * negocio: siRestorea el flag y se deja que el reintento siga su curso.
     */
    private void dormir() {
        try {
            Thread.sleep(esperaMs);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            log.debug("Se interrumpió la espera entre reintentos");
        }
    }
}