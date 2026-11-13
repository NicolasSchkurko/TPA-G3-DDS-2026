package ar.edu.utn.frba.ddsi.notificaciones.models.repositories.SpringRepositories;

import ar.edu.utn.frba.ddsi.notificaciones.services.LecturasNotificacionesService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.datasource.url=jdbc:h2:mem:lecturas-notificaciones-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LecturasNotificacionesService.class)
class LecturasNotificacionesPersistenceTest {

    @Autowired
    private LecturasNotificacionesService service;

    @Autowired
    private RepositorioLecturasNotificaciones repositorio;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persisteLecturasPorUsuarioYNoDuplicaAlRepetirLaOperacion() {
        String usuarioId = "usuario-keycloak-123";
        UUID primera = UUID.randomUUID();
        UUID segunda = UUID.randomUUID();

        service.marcarLeidas(usuarioId, Set.of(primera, segunda));
        service.marcarLeidas(usuarioId, Set.of(primera));
        repositorio.flush();
        entityManager.clear();

        assertEquals(Set.of(primera, segunda), Set.copyOf(service.listarLeidas(usuarioId)));
        assertEquals(List.of(), service.listarLeidas("otro-usuario"));
    }
}
