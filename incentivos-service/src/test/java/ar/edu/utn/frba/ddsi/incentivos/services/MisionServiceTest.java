package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.MisionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.OperacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.ReglaDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.MisionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Factory.OperacionFactory;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Operacion.Operaciones.SuperaCantidad;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.AtributoImpacto;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Reglas.Regla;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("MisionService")
class MisionServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();

    private RepositorioMisiones repoMisiones;
    private RepositorioPerfiles repoPerfiles;
    private SincronizacionPerfiles gestorSincronizacion;
    private MisionService service;

    @BeforeEach
    void setUp() {
        repoMisiones = mock(RepositorioMisiones.class);
        repoPerfiles = mock(RepositorioPerfiles.class);
        gestorSincronizacion = mock(SincronizacionPerfiles.class);
        service = new MisionService(
                repoMisiones,
                repoPerfiles,
                mock(RepositorioCategorias.class),
                new MisionFactory(new OperacionFactory()),
                gestorSincronizacion,
                mock(ValidadorAdmin.class)
        );
    }

    private static Mision misionVigente() {
        return new Mision("Diez dones", null, "Donar diez veces", "Constante",
                new Regla(null, AtributoImpacto.ESTADO, new SuperaCantidad(10, 1)));
    }

    private static MisionDTO dto(String descripcion, String insignia,
                                String atributo, Integer objetivo) {
        OperacionDTO operacion = new OperacionDTO("SUPERA_CANTIDAD", objetivo, null, 1);
        ReglaDTO regla = new ReglaDTO(null, atributo, operacion);
        MisionDTO dto = new MisionDTO();
        dto.setNombreMision("Diez dones");
        dto.setDescripcion(descripcion);
        dto.setInsigniaObjetivo(insignia);
        dto.setRegla(regla);
        return dto;
    }

    private Mision colonearEnRepositorio() {
        Mision mision = misionVigente();
        UUID idMision = UUID.randomUUID();
        ReflectionTestUtils.setField(mision, "idMision", idMision);
        when(repoMisiones.findById(idMision)).thenReturn(Optional.of(mision));
        when(repoMisiones.save(any(Mision.class))).thenAnswer(i -> i.getArgument(0));
        return mision;
    }

    @Nested
    @DisplayName("actualizar una misión no borra el avance de la gente")
    class ActualizarNoBorraElAvance {

        @Test
        @DisplayName("retocar la descripción no reinicia el progreso de nadie")
        void retocarLaDescripcionNoReiniciaElProgreso() {
            Mision mision = colonearEnRepositorio();
            UUID idMision = mision.getIdMision();

            service.actualizarMision(ADMIN, idMision,
                    dto("Donar diez veces, sin límite", "Constante", "ESTADO", 10));

            verify(gestorSincronizacion, never()).reiniciarProgresoDeMision(any());
        }

        @Test
        @DisplayName("cambiar solo el texto de la insignia tampoco reinicia el progreso")
        void cambiarLaInsigniaTampocoReiniciaElProgreso() {
            Mision mision = colonearEnRepositorio();
            UUID idMision = mision.getIdMision();

            service.actualizarMision(ADMIN, idMision,
                    dto("Donar diez veces", "Constante Editada", "ESTADO", 10));

            verify(gestorSincronizacion, never()).reiniciarProgresoDeMision(any());
        }

        @Test
        @DisplayName("cambiar el objetivo sí reinicia el progreso")
        void cambiarElObjetivoSiReiniciaElProgreso() {
            Mision mision = colonearEnRepositorio();
            UUID idMision = mision.getIdMision();

            service.actualizarMision(ADMIN, idMision,
                    dto("Donar diez veces", "Constante", "ESTADO", 20));

            verify(gestorSincronizacion).reiniciarProgresoDeMision(idMision);
        }

        @Test
        @DisplayName("cambiar el atributo de la regla sí reinicia el progreso")
        void cambiarElAtributoSiReiniciaElProgreso() {
            Mision mision = colonearEnRepositorio();
            UUID idMision = mision.getIdMision();

            service.actualizarMision(ADMIN, idMision,
                    dto("Donar diez veces", "Constante", "CATEGORIA", 10));

            verify(gestorSincronizacion).reiniciarProgresoDeMision(idMision);
        }
    }

    @Nested
    @DisplayName("eliminar una misión avisa antes de reventar")
    class EliminarMision {

        private UUID idMision;
        private Mision mision;

        @BeforeEach
        void preparar() {
            idMision = UUID.randomUUID();
            mision = misionVigente();
            ReflectionTestUtils.setField(mision, "idMision", idMision);
            when(repoMisiones.findById(idMision)).thenReturn(Optional.of(mision));
        }

        @Test
        @DisplayName("si no existe responde 404 y no 204")
        void siNoExisteResponde404() {
            UUID inexistente = UUID.randomUUID();
            when(repoMisiones.findById(inexistente)).thenReturn(Optional.empty());

            // Antes repoMisiones.eliminarMision devolvia null en silencio y el controller
            // respondia 204 igual, como si se hubiera borrado algo.
            assertThatThrownBy(() -> service.eliminarMision(ADMIN, inexistente))
                    .isInstanceOf(InexistenteException.class);

            verify(repoMisiones, never()).delete(any(Mision.class));
        }

        @Test
        @DisplayName("no se borra si alguien la está haciendo")
        void noSeBorraSiAlguienLaEstaHaciendo() {
            when(repoPerfiles.countByProgresoMisionActualMision(mision)).thenReturn(3L);

            assertThatThrownBy(() -> service.eliminarMision(ADMIN, idMision))
                    .isInstanceOf(ConflictoException.class)
                    .hasMessageContaining("3 donante(s)");

            verify(repoMisiones, never()).delete(any(Mision.class));
        }

        @Test
        @DisplayName("no se borra si alguien ya obtuvo su insignia")
        void noSeBorraSiAlguienYaObtuvoSuInsignia() {
            when(repoPerfiles.countByProgresoMisionActualMision(mision)).thenReturn(0L);
            when(repoPerfiles.countByInsigniasObtenidasInsignia(mision.getInsigniaObjetivo()))
                    .thenReturn(1L);

            assertThatThrownBy(() -> service.eliminarMision(ADMIN, idMision))
                    .isInstanceOf(ConflictoException.class)
                    .hasMessageContaining("ya obtuvieron su insignia");

            verify(repoMisiones, never()).delete(any(Mision.class));
        }

        @Test
        @DisplayName("se borra si nadie la está usando")
        void seBorraSiNadieLaEstaUsando() {
            when(repoPerfiles.countByProgresoMisionActualMision(mision)).thenReturn(0L);
            when(repoPerfiles.countByInsigniasObtenidasInsignia(mision.getInsigniaObjetivo()))
                    .thenReturn(0L);

            service.eliminarMision(ADMIN, idMision);

            verify(repoMisiones).delete(mision);
        }
    }
}
