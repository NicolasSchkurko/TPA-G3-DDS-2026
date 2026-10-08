package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.ActividadDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:actividad;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MetricasService.class)
class MetricaActividadJpaTest {

    @Autowired
    private MetricasService metricasService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("La actividad de un donante con donaciones devuelve los totales")
    void actividadDeUnDonanteConDonacionesDevuelveLosTotales() {
        UUID idUsuario = UUID.randomUUID();
        entityManager.persist(new Perfil(idUsuario, "Ana"));
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 1, 10, 0), 3, "Banco de alimentos");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 2, 11, 0), 5, "Banco de alimentos");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 11, 3, 9, 0), 1, "Comedor");
        entityManager.flush();
        entityManager.clear();

        ActividadDTO actividad = metricasService.obtenerEvolucionHistorica(idUsuario);

        assertThat(actividad.getRegistros()).hasSize(2);
        assertThat(actividad.getTotalDonaciones()).isEqualTo(3);
        assertThat(actividad.getTotalOrganizaciones()).isEqualTo(2);
    }

    @Test
    @DisplayName("La actividad de un donante sin donaciones devuelve totales en cero")
    void actividadSinDonacionesDevuelveTotalesEnCero() {
        UUID idUsuario = UUID.randomUUID();
        entityManager.persist(new Perfil(idUsuario, "Beto"));
        entityManager.flush();
        entityManager.clear();

        ActividadDTO actividad = metricasService.obtenerEvolucionHistorica(idUsuario);

        assertThat(actividad.getRegistros()).isEmpty();
        assertThat(actividad.getTotalDonaciones()).isZero();
        assertThat(actividad.getTotalOrganizaciones()).isZero();
    }

    private void persistirImpacto(UUID idUsuario,
                                  LocalDateTime fecha,
                                  int cantidadBienes,
                                  String entidad) {
        entityManager.persist(new ImpactoDonacion(
                UUID.randomUUID(), idUsuario, entidad, cantidadBienes, fecha,
                "Indumentaria", "Ropa", "ENTREGADA"));
    }
}
