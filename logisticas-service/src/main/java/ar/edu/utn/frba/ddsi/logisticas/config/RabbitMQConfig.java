package ar.edu.utn.frba.ddsi.logisticas.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Topología del broker de integración con donaciones-service.
 *
 * <p>Por defecto hay una sola cola compartida (si una instancia se cae, las demás siguen) y el
 * exchange es consistent-hash en los dos modos, así que donaciones-service no necesita saber
 * dónde está logística. Las colas de trabajo declaran dead letter exchange: lo no procesable
 * termina en {@link #DLQ_INTEGRACION}.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange de integracion por hash. Lo publica {@code donaciones-service}. */
    public static final String EXCHANGE_INTEGRACION = "logistica.integracion.hash";

    /** Exchange de los eventos de trazabilidad que este servicio publica. */
    public static final String EXCHANGE_EVENTOS = "logistica.eventos.exchange";

    /** Routing key de las donaciones nuevas que esperan planificarse. */
    public static final String RK_NUEVA_DONACION = "donaciones.creada";

    /** Routing key de los eventos de trazabilidad que este servicio publica. */
    public static final String RK_EVENTO = "logistica.evento";

    /** Clave del exchange consistent-hash. Debe coincidir con la de {@code donaciones-service}. */
    public static final String HEADER_PARTICION = "x-id-donacion";

    /** Cola unica de integracion, la que se usa cuando no hay particionado. */
    public static final String COLA_INTEGRACION = "logistica.integracion.queue";

    /** Prefijo de las colas particionadas: {@code logistica.integracion.queue.<n>}. */
    public static final String PREFIJO_COLA_INTEGRACION = COLA_INTEGRACION + ".";

    /** Dead letter de las colas de integracion. */
    public static final String DLQ_INTEGRACION = "logistica.integracion.dlq";

    private final int totalShards;
    private final List<Integer> shardsAsignadas;

    public RabbitMQConfig(
            @Value("${logistica.shards.total:4}") int totalShards,
            @Value("${logistica.shards.asignadas:}") String asignadas) {

        if (totalShards < 1) {
            throw new IllegalArgumentException(
                    "logistica.shards.total tiene que ser al menos 1, vino " + totalShards);
        }
        this.totalShards = totalShards;
        this.shardsAsignadas = particionado(asignadas)
                ? parsear(asignadas, totalShards)
                : List.of();
    }

    private static boolean particionado(String asignadas) {
        return asignadas != null && !asignadas.isBlank();
    }

    private static List<Integer> parsear(String asignadas, int total) {
        Set<Integer> elegidas = new LinkedHashSet<>();
        for (String trozo : asignadas.split(",")) {
            String limpio = trozo.trim();
            if (limpio.isEmpty()) {
                continue;
            }
            int indice;
            try {
                indice = Integer.parseInt(limpio);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("logistica.shards.asignadas tiene un valor "
                        + "que no es un numero: '" + limpio + "'");
            }
            if (indice < 0 || indice >= total) {
                throw new IllegalArgumentException("logistica.shards.asignadas tiene el shard "
                        + indice + " y solo hay " + total + " (de 0 a " + (total - 1) + ")");
            }
            elegidas.add(indice);
        }

        if (elegidas.isEmpty()) {
            throw new IllegalArgumentException(
                    "logistica.shards.asignadas no tiene ningun shard valido: esta instancia "
                            + "no atenderia ninguna cola y se perderian los mensajes");
        }
        return List.copyOf(elegidas);
    }

    /** Si el reparto entre instancias está activado (apagado por defecto: la cola compartida ya da disponibilidad).
     *  <p>Para activarlo, setear {@code logistica.shards.asignadas} repartido disjunto entre instancias
     *  ({@code instancia 1 -> 0,1}, {@code instancia 2 -> 2,3}). */
    public boolean modoParticionado() {
        return !shardsAsignadas.isEmpty();
    }

    /** Toda la topología en un solo {@link Declarables}: el número de colas depende del modo
     *  ({@code RabbitAdmin} solo recorre beans de tipo {@code Queue}, no colecciones de ellas). */
    @Bean
    public Declarables topologia() {
        CustomExchange integracion = new CustomExchange(EXCHANGE_INTEGRACION,
                "x-consistent-hash", true, false,
                Collections.singletonMap("hash-header", HEADER_PARTICION));
        TopicExchange eventos = new TopicExchange(EXCHANGE_EVENTOS, true, false);

        Queue muerta = QueueBuilder.durable(DLQ_INTEGRACION).build();

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(integracion);
        declarables.add(eventos);
        declarables.add(muerta);

        if (modoParticionado()) {
            declarables.addAll(colasParticionadas(integracion));
        } else {
            declarables.add(colaDeTrabajo(COLA_INTEGRACION));
            Queue colaIntegracion = colaDeTrabajo(COLA_INTEGRACION);
            declarables.add(colaIntegracion);
            declarables.add(bindingAlHash(integracion, colaIntegracion, 1));
        }

        return new Declarables(declarables);
    }

    /**
     * Las N colas del modo particionado. Las declaran todas las instancias (declarar una cola
     * durable idéntica es idempotente y garantiza que existan levantando una sola instancia).
     *
     * <p>La clave del binding va de 1 a N: el plugin de consistent-hash rechaza el cero con
     * {@code PRECONDITION_FAILED}.
     */
    private List<Declarable> colasParticionadas(CustomExchange integracion) {
        List<Declarable> declarables = new ArrayList<>();
        for (int i = 0; i < totalShards; i++) {
            Queue cola = colaDeTrabajo(nombreCola(i));
            declarables.add(cola);
            declarables.add(bindingAlHash(integracion, cola, i + 1));
        }
        return declarables;
    }

    private Queue colaDeTrabajo(String nombre) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ_INTEGRACION)
                .build();
    }

    private Binding bindingAlHash(CustomExchange integracion, Queue cola, int clave) {
        return BindingBuilder.bind(cola)
                .to(integracion)
                .with(String.valueOf(clave))
                .noargs();
    }

    /**
     * Las colas que atiende <b>esta</b> instancia.
     *
     * <p>Es lo que resuelve el {@code @RabbitListener}. Sin particionado devuelve la cola unica,
     * que es lo que hacen todas las instancias; con particionado devuelve las suyas.
     */
    @Bean
    public String[] colasDeEstaInstancia() {
        if (!modoParticionado()) {
            return new String[] {COLA_INTEGRACION};
        }

        String[] nombres = new String[shardsAsignadas.size()];
        for (int i = 0; i < shardsAsignadas.size(); i++) {
            nombres[i] = nombreCola(shardsAsignadas.get(i));
        }
        return nombres;
    }

    /** Los indices de shard que atiende esta instancia, vacio si no hay particionado. */
    public List<Integer> getShardsAsignadas() {
        return shardsAsignadas;
    }

    public int getTotalShards() {
        return totalShards;
    }

    public static String nombreCola(int shard) {
        return PREFIJO_COLA_INTEGRACION + shard;
    }

    /**
 * Serializa a JSON. No es opcional: sin este bean el listener recibe el body crudo y falla con
 * {@code MessageConversionException: Cannot convert from [[B] to [EntregaDTO]}.
 */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
