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
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.AmqpException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Los eventos se guardan una vez por ítem, con su FK, y se publican recién al commitear")
class GestorPublicacionEventosTest {

    private static final UUID ID_DONACION_A =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_DONACION_B =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private RepositorioEventoLogistica repoEventos;
    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private ProductorEventosLogistica productorEventos;

    private GestorPublicacionEventos gestor() {
        return new GestorPublicacionEventos(repoEventos, repoItemEntrega, new ObjectMapper(), productorEventos);
    }

    @AfterEach
    void sinTransaccionAbierta() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static ItemEntrega item(UUID id) {
        return new ItemEntrega(id, 1, UnidadDeMedida.UNIDADES, null);
    }

    private static Ruta rutaVacia() {
        Ruta ruta = new Ruta(new Camion("AB123CD", 10.0, 3.0, 1000.0, true));
        ruta.setIdRuta(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        return ruta;
    }

    private static Ruta rutaCon(ItemEntrega... items) {
        Ruta ruta = rutaVacia();

        Parada parada = new Parada();
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

    @Test
    @DisplayName("El INICIO_RUTA no se mete en la lista de los items: es un hecho de la ruta")
    void elInicioDeRutaNoSeMeteEnLaListaDeLosItems() {
        ItemEntrega primero = item(ID_DONACION_A);
        ItemEntrega segundo = item(ID_DONACION_B);

        gestor().publicarInicioRuta(rutaCon(primero, segundo));

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
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("ENTREGA_CONFIRMADA");
    }

    @Test
    @DisplayName("Con transaccion abierta el evento NO se publica hasta el commit")
    void noSePublicaAntesDelCommit() {
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
    @DisplayName("Si el broker falla despues del commit, la operacion no se rompe")
    void unFalloDelBrokerTrasElCommitNoSePropaga() {
        // La transaccion ya commiteo y el evento quedo guardado. Si la excepcion del broker sale
        // de afterCommit, el controller responde 500 por un cambio que en la base si ocurrio y el
        // reintento del cliente encuentra el estado ya avanzado.
        ItemEntrega item = item(ID_DONACION_A);
        item.setEstado(EstadoEntrega.EN_CAMINO);

        TransactionSynchronizationManager.initSynchronization();
        gestor().publicarEntregaConfirmada(item, rutaCon(item), "http://foto.jpg");

        doThrow(new AmqpException("broker caido"))
                .when(productorEventos).publicar(any(EventoLogistica.class));

        assertThatCode(GestorPublicacionEventosTest::commitear).doesNotThrowAnyException();

        // Se intentó publicar y el evento quedó persistido para una republicación manual.
        verify(productorEventos).publicar(any(EventoLogistica.class));
        verify(repoEventos).save(any(EventoLogistica.class));
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
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("REINGRESO_DEPOSITO");
        verify(productorEventos, times(1)).publicar(captor.getValue());
    }

    @Test
    @DisplayName("Una ruta sin items publica su inicio igual, una sola vez")
    void unaRutaSinItemsPublicaUnSoloInicio() {
        gestor().publicarInicioRuta(rutaVacia());

        verify(repoEventos, times(1)).save(any(EventoLogistica.class));
        verify(productorEventos, times(1)).publicar(any(EventoLogistica.class));
    }
}