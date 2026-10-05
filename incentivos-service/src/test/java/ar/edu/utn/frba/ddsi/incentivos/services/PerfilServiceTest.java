package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.MisionPerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.ProgresoMision;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("PerfilService: la mision vigente del donante viaja con su avance")
class PerfilServiceTest {

    private final PerfilService service = new PerfilService(
            mock(RepositorioPerfiles.class),
            mock(RepositorioCategorias.class),
            mock(RepositorioDonaciones.class),
            mock(DonacionClient.class)
    );

    private static ProgresoMision progresoDe(Integer avance, Integer objetivo) {
        Mision mision = new Mision(
                "Diez dones",
                null,
                "Donar diez veces",
                "Constante",
                new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(objetivo, 1))
        );

        ProgresoMision progreso = new ProgresoMision(mision);
        progreso.setProgreso(avance);
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