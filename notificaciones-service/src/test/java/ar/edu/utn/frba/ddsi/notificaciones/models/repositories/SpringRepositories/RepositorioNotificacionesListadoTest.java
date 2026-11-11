package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Mensaje.Mensaje;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.EstadoNotificacion;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion.Notificacion;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyReferenceException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Punto 26: el listado pagina/filtra y trae el mensaje en el mismo join (sin N+1). */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories.SqlInspector",
        "spring.datasource.url=jdbc:h2:mem:listado-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositorioNotificacionesListadoTest {

    @Autowired
    private RepositorioNotificaciones repositorio;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void limpiarInspector() {
        SqlInspector.SENTENCIAS.clear();
    }

    private Notificacion guardar(EstadoNotificacion estado, String asunto) {
        Notificacion notificacion = new Notificacion(
                "ana@test.com", "email", new Mensaje(asunto, "Cuerpo"));
        if (estado == EstadoNotificacion.ENVIADA) {
            notificacion.marcarEnviada();
        }
        return repositorio.saveAndFlush(notificacion);
    }

    @Test
    void filtraYPaginaPorEstado() {
        guardar(EstadoNotificacion.PENDIENTE, "A");
        guardar(EstadoNotificacion.PENDIENTE, "B");
        guardar(EstadoNotificacion.ENVIADA, "C");
        entityManager.clear();

        Page<Notificacion> primera = repositorio.findByEstado(
                EstadoNotificacion.PENDIENTE,
                PageRequest.of(0, 1, Sort.by("fechaCreacion")));
        Page<Notificacion> segunda = repositorio.findByEstado(
                EstadoNotificacion.PENDIENTE,
                PageRequest.of(1, 1, Sort.by("fechaCreacion")));

        assertEquals(2, primera.getTotalElements());
        assertEquals(1, primera.getContent().size());
        assertEquals(1, segunda.getContent().size());
        assertEquals(EstadoNotificacion.PENDIENTE, primera.getContent().get(0).getEstado());
    }

    @Test
    void buscarSinEstadoDevuelveTodasYConEstadoFiltra() {
        guardar(EstadoNotificacion.PENDIENTE, "A");
        guardar(EstadoNotificacion.PENDIENTE, "B");
        guardar(EstadoNotificacion.ENVIADA, "C");

        Page<Notificacion> todas = repositorio.buscar(
                null, PageRequest.of(0, 10, Sort.by("fechaCreacion")));
        Page<Notificacion> pendientes = repositorio.buscar(
                EstadoNotificacion.PENDIENTE, PageRequest.of(0, 10, Sort.by("fechaCreacion")));

        assertEquals(3, todas.getTotalElements());
        assertEquals(2, pendientes.getTotalElements());
    }

    @Test
    void findByIdSolicitudEsNullSafe() {
        Notificacion conSolicitud = new Notificacion("sol-1", "ana@test.com", "email",
                new Mensaje("Asunto", "Cuerpo"));
        repositorio.saveAndFlush(conSolicitud);

        assertTrue(repositorio.findByIdSolicitud(null).isEmpty());
        assertTrue(repositorio.findByIdSolicitud("  ").isEmpty());
        assertTrue(repositorio.findByIdSolicitud("sol-1").isPresent());
    }

    @Test
    void elListadoPorEstadoNoHaceSelectSeparadoDeMensajes() {
        guardar(EstadoNotificacion.PENDIENTE, "A");
        guardar(EstadoNotificacion.PENDIENTE, "B");
        entityManager.clear();
        SqlInspector.SENTENCIAS.clear();

        repositorio.findByEstado(EstadoNotificacion.PENDIENTE,
                PageRequest.of(0, 10, Sort.by("fechaCreacion")));

        assertFalse(SqlInspector.SENTENCIAS.isEmpty());
        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .anyMatch(sql -> sql.toLowerCase().contains("from notificaciones")),
                "la consulta tiene que ir a notificaciones: " + SqlInspector.SENTENCIAS);
        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .noneMatch(sql -> sql.toLowerCase().contains("from mensajes")),
                "el EntityGraph tiene que traer el mensaje en el join: " + SqlInspector.SENTENCIAS);
    }

    @Test
    void unSortInexistenteLanzaPropertyReferenceException() {
        assertThrows(PropertyReferenceException.class,
                () -> repositorio.findAll(PageRequest.of(0, 1, Sort.by("noExiste"))));
    }

    @Test
    void elListadoSinFiltroTampocoHaceSelectSeparadoDeMensajes() {
        guardar(EstadoNotificacion.PENDIENTE, "A");
        guardar(EstadoNotificacion.ENVIADA, "B");
        entityManager.clear();
        SqlInspector.SENTENCIAS.clear();

        repositorio.findAll(PageRequest.of(0, 10, Sort.by("fechaCreacion")));

        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .noneMatch(sql -> sql.toLowerCase().contains("from mensajes")),
                "el EntityGraph de findAll tiene que evitar el N+1: " + SqlInspector.SENTENCIAS);
    }

    private Notificacion guardarPara(String destinatario, EstadoNotificacion estado, String asunto) {
        Notificacion notificacion = new Notificacion(
                destinatario, "email", new Mensaje(asunto, "Cuerpo"));
        if (estado == EstadoNotificacion.ENVIADA) {
            notificacion.marcarEnviada();
        }
        return repositorio.saveAndFlush(notificacion);
    }

    @Test
    void listaPorDestinatarioFiltraPorContactoYDescartaALosDemas() {
        guardarPara("ana@test.com", EstadoNotificacion.PENDIENTE, "A");
        guardarPara("ANA@test.com", EstadoNotificacion.PENDIENTE, "B");
        guardarPara("otra@test.com", EstadoNotificacion.PENDIENTE, "C");

        Page<Notificacion> resultado = repositorio.findByDireccionDeContactoIgnoreCase(
                "ana@test.com", PageRequest.of(0, 10, Sort.by("fechaCreacion")));

        assertEquals(2, resultado.getTotalElements());
    }

    @Test
    void listaPorDestinatarioConEstadoFiltraAdemasPorEstado() {
        guardarPara("ana@test.com", EstadoNotificacion.ENVIADA, "Enviada");
        guardarPara("ana@test.com", EstadoNotificacion.PENDIENTE, "Pendiente");

        Page<Notificacion> resultado = repositorio.findByDireccionDeContactoIgnoreCaseAndEstado(
                "ana@test.com", EstadoNotificacion.ENVIADA,
                PageRequest.of(0, 10, Sort.by("fechaCreacion")));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Enviada", resultado.getContent().get(0).getMensaje().getAsunto());
    }

    @Test
    void elListadoPorDestinatarioNoHaceSelectSeparadoDeMensajes() {
        guardarPara("ana@test.com", EstadoNotificacion.PENDIENTE, "A");
        entityManager.clear();
        SqlInspector.SENTENCIAS.clear();

        repositorio.findByDireccionDeContactoIgnoreCase(
                "ana@test.com", PageRequest.of(0, 10, Sort.by("fechaCreacion")));

        assertTrue(SqlInspector.SENTENCIAS.stream()
                        .noneMatch(sql -> sql.toLowerCase().contains("from mensajes")),
                "el EntityGraph tiene que traer el mensaje en el join: " + SqlInspector.SENTENCIAS);
    }
}
