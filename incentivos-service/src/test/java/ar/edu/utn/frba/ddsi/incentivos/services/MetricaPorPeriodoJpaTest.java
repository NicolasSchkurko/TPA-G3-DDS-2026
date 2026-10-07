package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MetricaDonacionesDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:metrica;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.open-in-view=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MetricasService.class)
class MetricaPorPeriodoJpaTest {

    @Autowired
    private MetricasService metricasService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void metricaDeUnPeriodoConVariasDonacionesDevuelveElResumen() {
        UUID idUsuario = UUID.randomUUID();
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 1, 10, 0), 3, "Banco de alimentos");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 2, 11, 0), 5, "Banco de alimentos");
        entityManager.flush();
        entityManager.clear();

        MetricaDonacionesDTO dto = metricasService
                .obtenerMetrica(idUsuario, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
                .orElseThrow();

        assertThat(dto.getCantidadDonaciones()).isEqualTo(2);
        assertThat(dto.getCantidadBienes()).isEqualTo(8);
        assertThat(dto.getEntidadesBeneficiarias()).containsExactly("Banco de alimentos");
    }

    @Test
    void metricaDeUnDiaQueTieneDonacionesDevuelveElResumen() {
        UUID idUsuario = UUID.randomUUID();
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 7, 10, 0), 2, "Comedor");
        persistirImpacto(idUsuario, LocalDateTime.of(2026, 10, 7, 15, 0), 1, "Comedor");
        entityManager.flush();
        entityManager.clear();

        MetricaDonacionesDTO dto = metricasService
                .obtenerMetrica(idUsuario, LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 7))
                .orElseThrow();

        assertThat(dto.getCantidadDonaciones()).isEqualTo(2);
        assertThat(dto.getCantidadBienes()).isEqualTo(3);
    }

    @Test
    void unaDonacionJustoALasCeroDelDiaSiguienteNoEntraEnElPeriodo() {
        UUID idUsuario = UUID.randomUUID();
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 7);
        persistirImpacto(idUsuario, hasta.plusDays(1).atStartOfDay(), 9, "Comedor");
        persistirImpacto(idUsuario, hasta.atTime(23, 59, 59), 2, "Comedor");
        entityManager.flush();
        entityManager.clear();

        MetricaDonacionesDTO dto = metricasService
                .obtenerMetrica(idUsuario, desde, hasta)
                .orElseThrow();

        assertThat(dto.getCantidadDonaciones()).isEqualTo(1);
        assertThat(dto.getCantidadBienes()).isEqualTo(2);
        assertThat(dto.getEntidadesBeneficiarias()).containsExactly("Comedor");
    }

    @Test
    void metricaDeUnPeriodoSinDonacionesEsVacia() {
        UUID idUsuario = UUID.randomUUID();

        assertThat(metricasService.obtenerMetrica(
                idUsuario, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .isEmpty();
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
