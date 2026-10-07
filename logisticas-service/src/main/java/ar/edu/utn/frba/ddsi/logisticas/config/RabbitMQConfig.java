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
 * Topología del broker de integración con logisticas-service.
 *
 * <p><b>Por defecto hay una sola cola y todas las instancias la comparten.</b> Es lo correcto
 * para los mensajes que existen hoy: con una instancia caída, las otras siguen consumiendo y
 * no se traba nada. El reparto en varias colas queda como opción, apagada; ver
 * {@link #modoParticionado()}.
 *
 * <p><b>Por qué el exchange es consistent-hash si por defecto hay una sola cola.</b> Porque es lo
 * que permite que los dos modos usen el mismo exchange y, sobre todo, que
 * {@code donaciones-service} no tenga que saber en qué modo está logística. Con un solo binding,
 * el consistent-hash manda todo a esa cola; con cuatro bindings, lo reparte. El productor es
 * idéntico en los dos casos: publica al mismo exchange con el mismo encabezado.
 *
 * <p><b>Por qué el sondeo va a una cola aparte.</b> {@code SolicitudEventosListener} solo lee, y
 * la respuesta es la misma llegue donde llegue. Si compartiera cola con el trabajo de reparto,
 * todas las consultas caerían en el mismo shard por no traer encabezado de partición, y una
 * instancia se quedaría con todo el sondeo.
 *
 * <p><b>Los fallos no quedan dando vueltas.</b> Cada cola tiene dead letter exchange: lo que no se
 * puede procesar termina en {@link #DLQ_INTEGRACION} en vez de rebotar entre consumidores.
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /** Exchange de integracion por hash. Lo publica {@code donaciones-service}. */
    public static final String EXCHANGE_INTEGRACION = "logistica.integracion.hash";

    /** Exchange de integracion topic, para el sondeo de trazabilidad. */
    public static final String EXCHANGE_SONDEO = "logistica.exchange";

    /** Exchange de los eventos de trazabilidad que este servicio publica. */
    public static final String EXCHANGE_EVENTOS = "logistica.eventos.exchange";

    /** Routing key de las donaciones nuevas que esperan planificarse. */
    public static final String RK_NUEVA_DONACION = "donaciones.creada";

    /** Routing key de la solicitud de eventos de trazabilidad. */
    public static final String RK_SOLICITUD_EVENTOS = "logistica.solicitud.eventos";

    /** Routing key de los eventos de trazabilidad que este servicio publica. */
    public static final String RK_EVENTO = "logistica.evento";

    /**
     * Clave del exchange consistent-hash.
     *
     * <p>Tiene que coincidir con la constante del mismo nombre en {@code donaciones-service}: es
     * un contrato entre los dos servicios y por eso no se inventa en cada lado.
     */
    public static final String HEADER_PARTICION = "x-id-donacion";

    /** Cola unica de integracion, la que se usa cuando no hay particionado. */
    public static final String COLA_INTEGRACION = "logistica.integracion.queue";

    /** Prefijo de las colas particionadas: {@code logistica.integracion.queue.<n>}. */
    public static final String PREFIJO_COLA_INTEGRACION = COLA_INTEGRACION + ".";

    /** Dead letter de las colas de integracion. */
    public static final String DLQ_INTEGRACION = "logistica.integracion.dlq";

    /** Cola del sondeo, sin particionar: es de solo lectura. */
    public static final String COLA_SONDEO = "logistica.sondeo.queue";

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

    /**
     * Si el reparto entre instancias esta activado.
     *
     * <p><b>Apagado por defecto, y esa es la decision.</b> Con una sola cola compartida, si una
     * instancia se cae las otras siguen consumiendo: no se traba nada. Particionar compra orden
     * por donacion, pero hoy ningun mensaje lleva una transicion de estado (los dos que entran son
     * un alta idempotente y una consulta de solo lectura), asi que ese orden no se necesita.
     *
     * <p>Para activarlo hay que:setear {@code logistica.shards.asignadas} y repartir el total
     * entre las instancias de forma disjunto:
     *
     * <pre>
     *   instancia 1 -&gt; 0,1
     *   instancia 2 -&gt; 2,3
     * </pre>
     *
     * <p>Y conviene hacerlo recien cuando exista un mensaje con transiciones, porque si no se
     * paga la disponibilidad a cambio de un orden que no importa.
     */
    public boolean modoParticionado() {
        return !shardsAsignadas.isEmpty();
    }

    /**
     * Toda la topologia en un solo {@link Declarables}.
     *
     * <p><b>Por que uno solo y no un {@code @Bean} por cola.</b> El numero de colas depende del
     * modo, asi que no se pueden escribir N metodos a mano. Y un {@code @Bean} que devuelve
     * {@code List<Queue>} no lo declara el broker: {@code RabbitAdmin} solo recorre beans del tipo
     * {@code Queue}, no colecciones de ellos. Con eso las colas nunca se creaban y el listener
     * fallaba al arrancar con {@code 404 NOT_FOUND - no queue '...'}.
     */
    @Bean
    public Declarables topologia() {
        CustomExchange integracion = new CustomExchange(EXCHANGE_INTEGRACION,
                "x-consistent-hash", true, false,
                Collections.singletonMap("hash-header", HEADER_PARTICION));
        TopicExchange sondeo = new TopicExchange(EXCHANGE_SONDEO, true, false);
        TopicExchange eventos = new TopicExchange(EXCHANGE_EVENTOS, true, false);

        Queue muerta = QueueBuilder.durable(DLQ_INTEGRACION).build();
        Queue colaSondeo = colaDeTrabajo(COLA_SONDEO);

        List<Declarable> declarables = new ArrayList<>();
        declarables.add(integracion);
        declarables.add(sondeo);
        declarables.add(eventos);
        declarables.add(muerta);
        declarables.add(colaSondeo);

        // El sondeo no se particiona: es de solo lectura y no necesita ni orden ni reparto.
        declarables.add(BindingBuilder.bind(colaSondeo)
                .to(sondeo)
                .with(RK_SOLICITUD_EVENTOS));

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
     * Las N colas del modo particionado.
     *
     * <p><b>Las declara todas las instancias, aunque cada una atienda solo un subconjunto.</b>
     * Declarar una cola durable que ya existe y es identica no hace nada, asi que es idempotente;
     * y garantiza que existan aunque se levante una sola instancia con un shard suelto.
     *
     * <p><b>La clave del binding va de 1 a N, no de 0 a N-1.</b> El plugin de consistent-hash
     * rechaza el cero con {@code PRECONDITION_FAILED - The binding key must be greater than 0}.
     * El numero del shard en el nombre de la cola sigue siendo de 0 a N-1; la clave del binding
     * corre un numero y es interna del exchange.
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
     * Serializa a JSON.
     *
     * <p><b>Este bean no es opcional, y quitarlo rompe el consumo entero.</b> Sin el, Boot deja
     * el {@code RabbitTemplate} con {@code SimpleMessageConverter}, que solo sabe manejar
     * {@code byte[]}, {@code String} y {@code Serializable}. El listener recibe el body crudo y
     * falla con {@code MessageConversionException: Cannot convert from [[B] to [EntregaDTO]}.
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
