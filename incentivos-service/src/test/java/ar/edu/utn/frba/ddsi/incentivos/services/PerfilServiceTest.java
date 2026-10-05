package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.CantidadCoincidencias;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("PerfilService: la mision vigente del donante viaja con su avance")
class PerfilServiceTest {

    private final PerfilService service = new PerfilService(
            mock(RepositorioPerfiles.class),
            mock(RepositorioCategorias.class),
            mock(RepositorioDonaciones.class),
            mock(TransactionTemplate.class)
    );

    /**
     * Un progreso con el avance dado.
     *
     * <p>Se llega al avance con donaciones reales en vez de con un setter: la regla es
     * "N donaciones en estado ENTREGADA", así que cada donación entregada suma uno. Un
     * {@code avance} en null deja el progreso sin valor, que es el caso que cubre el
     * último test.
     */
    private static ProgresoMision progresoDe(Integer avance, Integer objetivo) {
        Mision mision = new Mision(
                "Diez dones",
                null,
                "Donar diez veces",
                "Constante",
                new Regla(null, AtributoImpacto.ESTADO,
                        new CantidadCoincidencias(objetivo, new ObjectMapper().valueToTree("ENTREGADA")))
        );

        ProgresoMision progreso = new ProgresoMision(mision);

        for (int i = 0; i < (avance == null ? 0 : avance); i++) {
            progreso.progresarMision(new ImpactoDonacion(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Fundacion", 3,
                    LocalDateTime.of(2026, 3, i + 1, 10, 0),
                    "ALIMENTOS", "MERCEARIA", "ENTREGADA"), List.of());
        }

        if (avance == null) {
            // Un progreso recien cargado de la base puede no tener el contador aun.
            ReflectionTestUtils.setField(progreso, "progreso", null);
        }

        return progreso;
    }

    @Test
    @DisplayName("la descripcion y la insignia objetivo ya no salen invertidas")
    void descripcionEInsigniaNoSalenInvertidas() {
        MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(2, 10));

        assertThat(dto.getNombreMision()).isEqualTo("Diez dones");
        assertThat(dto.getDescripcion()).isEqualTo("Donar diez veces");
        assertThat(dto.getInsigniaObjetivo()).isEqualTo("Constante");
    }

    @Test
    @DisplayName("expone cuanto lleva, cuanto necesita y cuanto le falta")
    void exponeActualObjetivoYFaltante() {
        MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(4, 10));

        assertThat(dto.getProgresoActual()).isEqualTo(4);
        assertThat(dto.getProgresoObjetivo()).isEqualTo(10);
        assertThat(dto.getProgresoFaltante()).isEqualTo(6);
    }

    @Test
    @DisplayName("una mision ya cumplida no muestra progreso faltante negativo")
    void unaMisionCumplidaNoTieneFaltanteNegativo() {
        MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(12, 10));

        assertThat(dto.getProgresoFaltante()).isZero();
    }

    @Test
    @DisplayName("un progreso sin valor cargado se muestra como cero, no como null")
    void unProgresoSinValorSeMuestraComoCero() {
        MisionPerfilDTO dto = service.convertirProgresoMisionADTO(progresoDe(null, 10));

        assertThat(dto.getProgresoActual()).isZero();
        assertThat(dto.getProgresoFaltante()).isEqualTo(10);
    }
}
