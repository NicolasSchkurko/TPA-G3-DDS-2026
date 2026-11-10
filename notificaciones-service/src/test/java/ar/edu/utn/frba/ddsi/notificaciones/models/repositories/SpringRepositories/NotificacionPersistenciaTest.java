package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Punto 40: un alta nueva va por persist y no por merge, así no hace SELECT previo por el id. */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.SqlInspector",
        "spring.datasource.url=jdbc:h2:mem:persistencia-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificacionPersistenciaTest {

    @Autowired
    private RepositorioNotificaciones repositorio;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void limpiarInspector() {
        SqlInspector.SENTENCIAS.clear();
    }

    @Test
    void guardarUnaNotificacionNuevaNoHaceSelectPrevio() {
        SqlInspector.SENTENCIAS.clear();
        Notificacion notificacion =
                new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo"));

        repositorio.saveAndFlush(notificacion);

        assertFalse(notificacion.isNew(), "@PostPersist tiene que marcarla como existente");
        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .anyMatch(sql -> sql.trim().toLowerCase().contains("insert into notificaciones")),
                "la notificación tiene que insertarse: " + SqlInspector.SENTENCIAS);
        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .noneMatch(sql -> sql.trim().toLowerCase().startsWith("select")),
                "sin Persistable, save() hace merge() y SELECT previo: " + SqlInspector.SENTENCIAS);
    }

    @Test
    void persisteYRecuperaElUrlMapa() {
        Notificacion guardada = repositorio.saveAndFlush(
                new Notificacion("ana@test.com", "email",
                        new Mensaje("Asunto", "Cuerpo", "https://mapa/1")));
        entityManager.clear();

        Notificacion recargada = repositorio.findById(guardada.getId()).orElseThrow();

        assertEquals("https://mapa/1", recargada.getMensaje().getUrlMapa());
    }

    @Test
    void unaNotificacionCargadaNoEsNueva() {
        Notificacion guardada = repositorio.saveAndFlush(
                new Notificacion("ana@test.com", "email", new Mensaje("Asunto", "Cuerpo")));
        entityManager.clear();

        Notificacion recargada = repositorio.findById(guardada.getId()).orElseThrow();

        assertFalse(recargada.isNew(), "@PostLoad tiene que marcar la fila como existente");
    }
}
