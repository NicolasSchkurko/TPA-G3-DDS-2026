package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica.EventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEventoLogistica;
import ar.edu.utn.frba.ddsi.logisticas.messaging.ProductorEventosLogistica;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * El evento tiene que quedar <b>guardado y publicado</b>, y no guardado dos veces ni publicado
 * por la mitad.
 *
 * <p>Cubre tres cosas que compartían el mismo archivo y la misma causa:
 *
 * <ul>
 *   <li>La relación ítem-evento apuntaba con {@code mappedBy="id"} a la clave primaria del
 *       propio evento, así que Hibernate armaba una relación que no existe y la lista salía
 *       siempre vacía.</li>
 *   <li>El mismo evento se metía en la lista de <i>todos</i> los ítems de la ruta.</li>
 *   <li>La publicación al broker pasaba dentro de la transacción, así que un rollback dejaba el
 *       mensaje afuera de una entrega que nunca ocurrió.</li>
 * </ul>
 *
 * <p>El mapeo se verifica por reflexión porque el módulo no tiene una base embebida en el build
 * offline: lo que se afirma es que la declaración JPA apunta a la FK real, que es exactamente
 * lo que estaba mal.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Los eventos se guardan una vez por ítem, con su FK, y se publican recién al commitear")
class GestorPublicacionEventosTest {

    private static final UUID ID_DONACION_A =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_DONACION_B =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private RepositorioEventoLogistica repoEventos;
    @Mock private ProductorEventosLogistica productorEventos;

    private GestorPublicacionEventos gestor() {
        return new GestorPublicacionEventos(repoEventos, new ObjectMapper(), productorEventos);
    }

    @AfterEach
    void sinTransaccionAbierta() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static ItemEntrega item(UUID id) {
        ItemEntrega item = new ItemEntrega(id, 1, UnidadDeMedida.UNIDADES, null);
        return item;
    }

    private static Ruta rutaVacia() {
        Ruta ruta = new Ruta(new Camion("AB123CD", 10.0, 3.0, 1000.0, true));
        ruta.setIdRuta(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return ruta;
    }

    /**
     * Arma una parada a mano en vez de con {@code new Parada(item)}.
     *
     * <p>El constructor de {@code Parada} lee la entidad del ítem para sacar la dirección, y acá
     * la entidad es null porque a estos tests lo que les interesa es el evento y su FK, no la
     * jerarquía de direcciones. La parada se llena por {@code agregarItem}, que es lo mismo que
     * hace el planificador.
     */
    private static Ruta rutaCon(ItemEntrega... items) {
        Ruta ruta = rutaVacia();

        Parada parada = new Parada();
        parada.setRuta(ruta);
        for (ItemEntrega item : items) {
            parada.agregarItem(item);
        }
        ruta.getParadas().add(parada);

        return ruta;
    }

    /** Dispara lo que Spring dispararía al commitear, sin levantar un contexto. */
    private static void commitear() {
        for (TransactionSynchronization sincronizacion
                : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCommit();
            sincronizacion.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        TransactionSynchronizationManager.clearSynchronization();
    }

    // --- El mapeo: la FK real ---

    @Test
    @DisplayName("La relacion item-evento apunta a la FK real, no a la clave primaria del evento")
    void laRelacionApuntaALaFkReal() throws NoSuchFieldException {
        // mappedBy="id" no es un atributo que referencie al item: id es la PK del propio evento
        // (id_evento, IDENTITY). Hibernate armaba una relacion inventada con la FK en
        // item_entrega.id_evento, siempre en NULL, y la lista salia vacia siempre.
        Field eventos = ItemEntrega.class.getDeclaredField("eventos");
        OneToMany relacion = eventos.getAnnotation(OneToMany.class);

        assertThat(relacion.mappedBy()).isEqualTo("item");

        // Y del lado dueño tiene que existir el ManyToOne que apunta al item, con la FK
        // escrita sobre id_donacion.
        Field item = EventoLogistica.class.getDeclaredField("item");
        assertThat(item.getAnnotation(ManyToOne.class)).isNotNull();
        assertThat(item.getAnnotation(JoinColumn.class).name()).isEqualTo("id_donacion");
    }

    /**
     * El cascade tiene que seguir, porque es lo único que permite borrar un ítem con historial.
     *
     * <p>Sin cascade ni orphanRemoval, la FK nueva vuelve imposible el DELETE: MySQL rechaza
     * borrar un ítem que tiene eventos, y {@code DELETE /entregas/{id} devolvería 500 para
     * cualquier entrega que hubiera pasado por logística.
     */
    @Test
    @DisplayName("La lista de eventos conserva el cascade, para que el item se pueda borrar")
    void laListaConservaElCascade() throws NoSuchFieldException {
        OneToMany relacion = ItemEntrega.class.getDeclaredField("eventos")
                .getAnnotation(OneToMany.class);

        assertThat(relacion.cascade())
                .as("sin cascade, borrar un item con eventos viola la FK")
                .contains(CascadeType.ALL);
        assertThat(relacion.orphanRemoval()).isTrue();
    }

    // --- El inicio de ruta es de la ruta, no de cada item ---

    @Test
    @DisplayName("El INICIO_RUTA no se mete en la lista de los items: es un hecho de la ruta")
    void elInicioDeRutaNoSeMeteEnLaListaDeLosItems() {
        // Antes era una sola instancia de EventoLogistica metida en las listas de todos los
        // items de la ruta. Con mappedBy="item" cada fila pertenece a un item, asi que no hay a
        // que FK asignarle una instancia compartida.
        ItemEntrega primero = item(ID_DONACION_A);
        ItemEntrega segundo = item(ID_DONACION_B);

        gestor().publicarInicioRuta(rutaCon(primero, segundo));

        // Una sola fila por inicio de ruta. Clonar el evento por item multiplicaria las filas
        // que devuelve el polling, con el mismo payload y el mismo referenciaId.
        verify(repoEventos, times(1)).save(any(EventoLogistica.class));
        assertThat(primero.getEventos()).isEmpty();
        assertThat(segundo.getEventos()).isEmpty();
    }

    @Test
    @DisplayName("El evento de inicio se guarda con el id de la ruta como referencia")
    void elInicioDeRutaGuardaUnaSolaFila() {
        Ruta ruta = rutaCon(item(ID_DONACION_A));

        gestor().publicarInicioRuta(ruta);

        ArgumentCaptor<EventoLogistica> captor = ArgumentCaptor.forClass(EventoLogistica.class);
        verify(repoEventos).save(captor.capture());

        assertThat(captor.getValue().getTipoEvento()).isEqualTo("INICIO_RUTA");
        assertThat(captor.getValue().getReferenciaId()).isEqualTo(ruta.getIdRuta().toString());
        // No pertenece a ningun item en particular: pertenece a la ruta.
        assertThat(captor.getValue().getItem()).isNull();
    }

    @Test
    @DisplayName("Al broker sale un solo INICIO_RUTA, aunque la ruta lleve varios items")
    void alBrokerSaleUnSoloInicioDeRuta() {
        // El consumidor notifica una vez por mensaje: N mensajes serian N notificaciones del
        // mismo inicio de ruta.
        gestor().publicarInicioRuta(rutaCon(item(ID_DONACION_A), item(ID_DONACION_B)));

        verify(productorEventos, times(1)).publicar(any(EventoLogistica.class));
    }

    @Test
    @DisplayName("Una entrega confirmada guarda su evento con la FK al item, no con la del otro")
    void laEntregaConfirmadaGuardaConSuFk() {
        ItemEntrega item = item(ID_DONACION_A);
        item.setEstado(EstadoEntrega.EN_CAMINO);

        gestor().publicarEntregaConfirmada(item, rutaCon(item), "http://foto.jpg");

        ArgumentCaptor<EventoLogistica> captor = ArgumentCaptor.forClass(EventoLogistica.class);
        verify(repoEventos).save(captor.capture());

        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("ENTREGA_CONFIRMADA");
    }

    // --- Publicar despues del commit (punto 28) ---

    @Test
    @DisplayName("Con transaccion abierta el evento NO se publica hasta el commit")
    void noSePublicaAntesDelCommit() {
        // Publicar dentro de la transaccion hacia que un rollback posterior dejara el mensaje
        // afuera igual: la base volvia a su estado y donaciones-service ya habia notificado una
        // entrega que en logistica nunca ocurrio.
        ItemEntrega item = item(ID_DONACION_A);
        item.setEstado(EstadoEntrega.EN_CAMINO);

        TransactionSynchronizationManager.initSynchronization();
        gestor().publicarEntregaConfirmada(item, rutaCon(item), "http://foto.jpg");

        // Guardado en la base: eso si tiene que pasar ya, es la bitacora.
        verify(repoEventos).save(any(EventoLogistica.class));
        // Pero todavia no salio al broker.
        verify(productorEventos, never()).publicar(any(EventoLogistica.class));

        commitear();

        verify(productorEventos, times(1)).publicar(any(EventoLogistica.class));
    }

    @Test
    @DisplayName("Un rollback deja el evento sin publicar: es el fantasma que se iba")
    void unRollbackNoPublica() {
        ItemEntrega item = item(ID_DONACION_A);
        item.setEstado(EstadoEntrega.EN_CAMINO);

        TransactionSynchronizationManager.initSynchronization();
        gestor().publicarEntregaConfirmada(item, rutaCon(item), "http://foto.jpg");

        // Spring dispara afterCompletion(ROLLED_BACK) y NO despacha afterCommit.
        for (TransactionSynchronization sincronizacion
                : TransactionSynchronizationManager.getSynchronizations()) {
            sincronizacion.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        TransactionSynchronizationManager.clearSynchronization();

        verify(productorEventos, never()).publicar(any(EventoLogistica.class));
    }

    @Test
    @DisplayName("Sin transaccion abierta se publica en el momento, no se pierde el evento")
    void sinTransaccionSePublicaIgual() {
        // Agendar la publicacion para un commit que no existe la perderia: es lo que pasa con
        // las llamadas que no abren transaccion.
        ItemEntrega item = item(ID_DONACION_A);
        item.setEstado(EstadoEntrega.EN_CAMINO);

        gestor().publicarEntregaConfirmada(item, rutaCon(item), "http://foto.jpg");

        verify(productorEventos, times(1)).publicar(any(EventoLogistica.class));
    }

    // --- El reingreso a deposito usa el mismo camino ---

    @Test
    @DisplayName("El reingreso a deposito tambien guarda con la FK y publica al commit")
    void elReingresoGuardaConSuFk() {
        ItemEntrega item = item(ID_DONACION_A);
        item.setEstado(EstadoEntrega.NO_RECIBIDA);

        gestor().publicarReingresoDeposito(item);

        ArgumentCaptor<EventoLogistica> captor = ArgumentCaptor.forClass(EventoLogistica.class);
        verify(repoEventos).save(captor.capture());

        assertThat(captor.getValue().getItem()).isSameAs(item);
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("REINGRESO_DEPOSITO");
        verify(productorEventos, times(1)).publicar(captor.getValue());
    }

    /**
     * El inicio de ruta se publica aunque la ruta no lleve ítems todavía.
     *
     * <p>El hecho de que arrancó la ruta es real y notificarlo está bien: el payload lleva la
     * URL de seguimiento, y una ruta recién planificada puede no tener ítems asignados en el
     * momento en que el chofer la arranca. Lo que no corresponde es inventar un evento por cada
     * ítem de una lista vacía, que era lo que hacía la versión anterior de este método.
     */
    @Test
    @DisplayName("Una ruta sin items publica su inicio igual, una sola vez")
    void unaRutaSinItemsPublicaUnSoloInicio() {
        gestor().publicarInicioRuta(rutaVacia());

        verify(repoEventos, times(1)).save(any(EventoLogistica.class));
        verify(productorEventos, times(1)).publicar(any(EventoLogistica.class));
    }
}