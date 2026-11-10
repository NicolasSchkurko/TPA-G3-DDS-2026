package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El reclamo del reintento automático es un compare-and-set: una sola instancia gana cada intento. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.SqlInspector",
        "spring.datasource.url=jdbc:h2:mem:notificaciones-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositorioNotificacionesReintentoTest {

    @Autowired
    private RepositorioNotificaciones repositorio;

    @AfterEach
    void limpiarInspector() {
        SqlInspector.SENTENCIAS.clear();
    }

    private Notificacion guardarPendiente() {
        return repositorio.save(new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo")));
    }

    @Test
    void elReclamoEsUnCompareAndSetSobreLosIntentos() {
        Notificacion notificacion = guardarPendiente();
        assertEquals(0, notificacion.getIntentos(), "las nuevas nacen con intentos en 0");

        assertEquals(1, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 0, 3));
        // Otra instancia que leyó intentos=0 ya no puede reclamar.
        assertEquals(0, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 0, 3));
        // La que leyó el valor actualizado sí.
        assertEquals(1, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 1, 3));
        // Tope: con 2 intentos y tope 2, no reclama.
        assertEquals(0, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 2, 2));
    }

    @Test
    void noReclamaSiLaNotificacionYaNoEstaPendiente() {
        Notificacion notificacion = guardarPendiente();
        notificacion.marcarEnviada();
        repositorio.save(notificacion);

        assertEquals(0, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 0, 3));
    }

    @Test
    void compensarDevuelveElIntentoConsumido() {
        Notificacion notificacion = guardarPendiente();
        assertEquals(1, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 0, 3));

        assertEquals(1, repositorio.compensarReintentoFallido(
                notificacion.getId(), EstadoNotificacion.PENDIENTE));
        // Con el intento devuelto, se puede volver a reclamar con el mismo valor leído.
        assertEquals(1, repositorio.reclamarParaReintento(
                notificacion.getId(), EstadoNotificacion.PENDIENTE, 0, 3));
    }

    @Test
    void losCierresSoloPisanElEstadoEnviandoConElClaimVigente() {
        Notificacion notificacion = guardarPendiente();
        LocalDateTime ahora = LocalDateTime.now();

        assertEquals(0, repositorio.marcarEnviadaSiEnviando(
                notificacion.getId(), null, ahora, EstadoNotificacion.ENVIADA, EstadoNotificacion.ENVIANDO));

        notificacion.marcarEnviando(ahora);
        UUID claim = notificacion.getClaimId();
        repositorio.saveAndFlush(notificacion);

        assertEquals(0, repositorio.marcarEnviadaSiEnviando(
                notificacion.getId(), UUID.randomUUID(), ahora,
                EstadoNotificacion.ENVIADA, EstadoNotificacion.ENVIANDO));
        assertEquals(1, repositorio.marcarEnviadaSiEnviando(
                notificacion.getId(), claim, ahora,
                EstadoNotificacion.ENVIADA, EstadoNotificacion.ENVIANDO));
        Notificacion cerrada = repositorio.findById(notificacion.getId()).orElseThrow();
        assertEquals(EstadoNotificacion.ENVIADA, cerrada.getEstado());
        assertNotNull(cerrada.getFechaEnvio());
        assertNull(cerrada.getFechaIntento());
        assertNull(cerrada.getClaimId());
    }

    @Test
    void marcarFallidaSiEnviandoSoloPisaElEstadoEnviandoConElClaimVigente() {
        Notificacion notificacion = guardarPendiente();
        LocalDateTime ahora = LocalDateTime.now();
        notificacion.marcarEnviando(ahora);
        UUID claim = notificacion.getClaimId();
        repositorio.saveAndFlush(notificacion);

        assertEquals(0, repositorio.marcarFallidaSiEnviando(
                notificacion.getId(), UUID.randomUUID(), EstadoNotificacion.FALLIDA,
                EstadoNotificacion.ENVIANDO));
        assertEquals(1, repositorio.marcarFallidaSiEnviando(
                notificacion.getId(), claim, EstadoNotificacion.FALLIDA, EstadoNotificacion.ENVIANDO));
        assertEquals(EstadoNotificacion.FALLIDA,
                repositorio.findById(notificacion.getId()).orElseThrow().getEstado());
        assertEquals(0, repositorio.marcarFallidaSiEnviando(
                notificacion.getId(), claim, EstadoNotificacion.FALLIDA, EstadoNotificacion.ENVIANDO));
    }

    @Test
    void liberarEnviosColgadosDevuelveAPendienteSoloLosViejosYCuentaElIntento() {
        LocalDateTime ahora = LocalDateTime.now();
        Notificacion vieja = guardarPendiente();
        vieja.marcarEnviando(ahora.minusMinutes(5));
        repositorio.saveAndFlush(vieja);
        Notificacion nueva = guardarPendiente();
        nueva.marcarEnviando(ahora);
        repositorio.saveAndFlush(nueva);

        assertEquals(1, repositorio.liberarEnviosColgados(
                EstadoNotificacion.PENDIENTE, EstadoNotificacion.ENVIANDO,
                ahora.minusMinutes(2)));

        Notificacion liberada = repositorio.findById(vieja.getId()).orElseThrow();
        assertEquals(EstadoNotificacion.PENDIENTE, liberada.getEstado());
        assertEquals(1, liberada.getIntentos(), "el ciclo colgado también consume un intento");
        assertEquals(EstadoNotificacion.ENVIANDO,
                repositorio.findById(nueva.getId()).orElseThrow().getEstado());
    }

    @Test
    void liberarEnviosColgadosIncluyeFechaNulaYNoSeTrabaConIntentosAlTope() {
        LocalDateTime ahora = LocalDateTime.now();
        Notificacion sinFecha = guardarPendiente();
        sinFecha.marcarEnviando(ahora);
        sinFecha.setFechaIntento(null);
        sinFecha.setIntentos(3);
        repositorio.saveAndFlush(sinFecha);

        assertEquals(1, repositorio.liberarEnviosColgados(
                EstadoNotificacion.PENDIENTE, EstadoNotificacion.ENVIANDO, ahora.minusMinutes(2)));

        Notificacion liberada = repositorio.findById(sinFecha.getId()).orElseThrow();
        assertEquals(EstadoNotificacion.PENDIENTE, liberada.getEstado());
        assertEquals(4, liberada.getIntentos(), "la liberación no se capa por intentos");
    }

    @Test
    void findParaReintentarFiltraPorEstadoYAntiguedad() {
        Notificacion vieja = guardarPendiente();
        vieja.setFechaCreacion(LocalDateTime.now().minusHours(1));
        repositorio.save(vieja);
        guardarPendiente(); // recién creada, no es candidata

        List<NotificacionReintentable> candidatas = repositorio.findParaReintentar(
                EstadoNotificacion.PENDIENTE, LocalDateTime.now().minusMinutes(10), 3,
                PageRequest.of(0, 10));

        assertEquals(1, candidatas.size());
        NotificacionReintentable candidata = candidatas.get(0);
        assertEquals(vieja.getId(), candidata.id());
        assertEquals(0, candidata.intentos());
        assertEquals("email", candidata.tipoMedioDeContacto());
        assertEquals("ana@test.com", candidata.direccionDeContacto());
    }

    @Test
    void findParaReintentarExcluyeLasQueAgotaronElTope() {
        Notificacion capada = guardarPendiente();
        capada.setFechaCreacion(LocalDateTime.now().minusHours(1));
        capada.setIntentos(3);
        repositorio.save(capada);

        List<NotificacionReintentable> candidatas = repositorio.findParaReintentar(
                EstadoNotificacion.PENDIENTE, LocalDateTime.now().minusMinutes(10), 3,
                PageRequest.of(0, 10));

        assertTrue(candidatas.isEmpty());
    }

    @Test
    void findParaReintentarRespetaElTamanoDelLote() {
        for (int i = 0; i < 3; i++) {
            Notificacion vieja = guardarPendiente();
            vieja.setFechaCreacion(LocalDateTime.now().minusHours(1));
            repositorio.save(vieja);
        }

        List<NotificacionReintentable> lote = repositorio.findParaReintentar(
                EstadoNotificacion.PENDIENTE, LocalDateTime.now().minusMinutes(10), 3,
                PageRequest.of(0, 2));

        assertEquals(2, lote.size());
    }

    @Test
    void findParaReintentarIncluyeFilasConIntentosNulos() {
        Notificacion vieja = guardarPendiente();
        vieja.setFechaCreacion(LocalDateTime.now().minusHours(1));
        vieja.setIntentos(null);
        repositorio.saveAndFlush(vieja);

        List<NotificacionReintentable> candidatas = repositorio.findParaReintentar(
                EstadoNotificacion.PENDIENTE, LocalDateTime.now().minusMinutes(10), 3,
                PageRequest.of(0, 10));

        assertEquals(1, candidatas.size());
        assertNull(candidatas.get(0).intentos());
    }

    @Test
    void findParaReintentarNoCargaElMensaje() {
        Notificacion vieja = guardarPendiente();
        vieja.setFechaCreacion(LocalDateTime.now().minusHours(1));
        repositorio.saveAndFlush(vieja);
        SqlInspector.SENTENCIAS.clear();

        repositorio.findParaReintentar(EstadoNotificacion.PENDIENTE,
                LocalDateTime.now().minusMinutes(10), 3, PageRequest.of(0, 10));

        assertFalse(SqlInspector.SENTENCIAS.isEmpty(), "el inspector tiene que haber capturado la consulta");
        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .anyMatch(sql -> sql.toLowerCase().contains("notificaciones")),
                "la consulta tiene que ir a notificaciones: " + SqlInspector.SENTENCIAS);
        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .noneMatch(sql -> sql.toLowerCase().contains("mensajes")),
                "la proyección no debe arrastrar el Mensaje EAGER: " + SqlInspector.SENTENCIAS);
    }
}
