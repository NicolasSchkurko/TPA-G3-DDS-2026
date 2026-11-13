package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Punto 21: el bloqueo pesimista serializa dos entregas concurrentes del mismo aviso. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:bloqueo-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RepositorioNotificacionesBloqueoTest {

    @Autowired
    private RepositorioNotificaciones repositorio;

    @Autowired
    private PlatformTransactionManager gestorTransacciones;

    @Test
    @Timeout(15)
    void dosEntregasConcurrentesSeSerializanYLaSegundaVeElEstadoEnviado() throws Exception {
        Notificacion notificacion = repositorio.save(
                new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo")));
        TransactionTemplate transaccion = new TransactionTemplate(gestorTransacciones);
        CountDownLatch primeraConBloqueo = new CountDownLatch(1);
        CountDownLatch liberarPrimera = new CountDownLatch(1);
        CountDownLatch segundaIntenta = new CountDownLatch(1);
        AtomicReference<EstadoNotificacion> estadoVistoPorLaSegunda = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Future<?> primera = pool.submit(() -> transaccion.executeWithoutResult(estado -> {
                Notificacion bloqueada = repositorio.findByIdParaEnvio(notificacion.getId())
                        .orElseThrow();
                primeraConBloqueo.countDown();
                esperar(liberarPrimera);
                bloqueada.marcarEnviada();
            }));

            assertTrue(primeraConBloqueo.await(5, TimeUnit.SECONDS));

            Future<?> segunda = pool.submit(() -> transaccion.executeWithoutResult(estado -> {
                segundaIntenta.countDown();
                estadoVistoPorLaSegunda.set(repositorio.findByIdParaEnvio(notificacion.getId())
                        .orElseThrow()
                        .getEstado());
            }));

            assertTrue(segundaIntenta.await(5, TimeUnit.SECONDS));
            // La primera todavía no commiteó: si no hay lock, la segunda termina enseguida.
            assertThrows(TimeoutException.class, () -> segunda.get(2, TimeUnit.SECONDS));

            liberarPrimera.countDown();
            primera.get(5, TimeUnit.SECONDS);
            segunda.get(5, TimeUnit.SECONDS);

            assertEquals(EstadoNotificacion.ENVIADA, estadoVistoPorLaSegunda.get());
        } finally {
            liberarPrimera.countDown();
            pool.shutdownNow();
        }
    }

    private static void esperar(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
        }
    }
}
