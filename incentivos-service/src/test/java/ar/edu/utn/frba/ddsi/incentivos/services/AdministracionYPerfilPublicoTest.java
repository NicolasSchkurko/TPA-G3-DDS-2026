package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.PerfilDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilPublicoDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Ranking.RankingMensual;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioRankings;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Puntos 21 y 8: control de admin y vista pública")
class AdministracionYPerfilPublicoTest {

    private static final UUID ADMIN = UUID.randomUUID();

    private RepositorioRankings repoRankings;
    private ValidadorAdmin validadorAdmin;
    private RankingService rankingService;

    @BeforeEach
    void setUp() {
        repoRankings = mock(RepositorioRankings.class);
        validadorAdmin = mock(ValidadorAdmin.class);
        rankingService = new RankingService(
                repoRankings,
                mock(RepositorioPerfiles.class),
                validadorAdmin);
    }

    @Nested
    @DisplayName("Punto 21: crear y borrar rankings exigen administrador")
    class RutasDeRanking {

        @Test
        @DisplayName("crear un ranking verifica los permisos del administrador")
        void crearVerificaPermisos() {
            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());

            rankingService.crearRankingMensual(ADMIN, YearMonth.of(2026, 3));

            verify(validadorAdmin).verificarPermisos(ADMIN);
        }

        @Test
        @DisplayName("crear un ranking NO guarda si el administrador no tiene permisos")
        void crearNoGuardaSiNoTienePermisos() {
            // verificarPermisos lanza SecurityException si no es admin.
            Mockito.doThrow(new SecurityException("no es admin"))
                   .when(validadorAdmin).verificarPermisos(ADMIN);

            assertThatThrownBy(() -> rankingService.crearRankingMensual(ADMIN, YearMonth.of(2026, 3)))
                    .isInstanceOf(SecurityException.class);

            verify(repoRankings, never()).save(any());
        }

        @Test
        @DisplayName("borrar un ranking verifica los permisos del administrador")
        void borrarVerificaPermisos() {
            when(repoRankings.existsById(any())).thenReturn(true);

            rankingService.eliminarRanking(ADMIN, UUID.randomUUID());

            verify(validadorAdmin).verificarPermisos(ADMIN);
        }

        @Test
        @DisplayName("borrar un ranking NO borra si el administrador no tiene permisos")
        void borrarNoBorraSiNoTienePermisos() {
            Mockito.doThrow(new SecurityException("no es admin"))
                   .when(validadorAdmin).verificarPermisos(ADMIN);

            assertThatThrownBy(() -> rankingService.eliminarRanking(ADMIN, UUID.randomUUID()))
                    .isInstanceOf(SecurityException.class);

            verify(repoRankings, never()).deleteById(any());
        }

        @Test
        @DisplayName("el scheduler genera el ranking sin pedir administrador")
        void elSchedulerNoPideAdmin() {
            // El scheduler corre dentro del proceso: no hay request ni header. Tiene que
            // poder generar el ranking mensual sin inventar un id de admin.
            when(repoRankings.findByPeriodo(any())).thenReturn(Optional.empty());
            when(repoRankings.save(any(RankingMensual.class)))
                    .thenAnswer(i -> i.getArgument(0));

            rankingService.crearRankingMensualActual();

            verify(validadorAdmin, never()).verificarPermisos(any());
        }
    }

    @Nested
    @DisplayName("Punto 8: la categoría es visible públicamente")
    class PerfilPublico {

        private RepositorioPerfiles repoPerfiles;
        private PerfilService perfilService;

        @BeforeEach
        void setUpPerfil() {
            repoPerfiles = mock(RepositorioPerfiles.class);
            perfilService = new PerfilService(
                    repoPerfiles,
                    mock(RepositorioCategorias.class),
                    mock(RepositorioDonaciones.class),
                    mock(TransactionTemplate.class),
                    mock(ValidadorAdmin.class));
        }

        @Test
        @DisplayName("devuelve el nombre de usuario y el de su categoría")
        void devuelveNombreYCategoria() {
            UUID idUsuario = UUID.randomUUID();
            Categoria categoria = new Categoria("Transformador", null, 1, List.of());

            Perfil perfil = new Perfil(idUsuario, "Ana");
            perfil.iniciarEn(categoria);
            when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.of(perfil));

            PerfilPublicoDTO dto = perfilService.obtenerPerfilPublico(idUsuario);

            assertThat(dto.getNombreUsuario()).isEqualTo("Ana");
            assertThat(dto.getNombreCategoria()).isEqualTo("Transformador");
        }

        @Test
        @DisplayName("un perfil sin categoría responde igual, con la categoría en null")
        void sinCategoriaRespondeConNull() {
            UUID idUsuario = UUID.randomUUID();
            Perfil perfil = new Perfil(idUsuario, "Beto");
            when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.of(perfil));

            PerfilPublicoDTO dto = perfilService.obtenerPerfilPublico(idUsuario);

            // El donante existe, asi que no es un 404: su nombre tiene que poder verse.
            assertThat(dto.getNombreUsuario()).isEqualTo("Beto");
            assertThat(dto.getNombreCategoria()).isNull();
        }

        @Test
        @DisplayName("si el donante no existe responde 404")
        void siNoExisteResponde404() {
            UUID idUsuario = UUID.randomUUID();
            when(repoPerfiles.findById(idUsuario)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> perfilService.obtenerPerfilPublico(idUsuario))
                    .isInstanceOf(InexistenteException.class);
        }
    }

    @Nested
    @DisplayName("Punto 39: editar y borrar perfiles exigen administrador")
    class EscriturasDePerfil {

        private RepositorioPerfiles repoPerfiles;
        private ValidadorAdmin validadorAdmin;
        private PerfilService perfilService;

        @BeforeEach
        void setUpPerfil() {
            repoPerfiles = mock(RepositorioPerfiles.class);
            validadorAdmin = mock(ValidadorAdmin.class);
            perfilService = new PerfilService(
                    repoPerfiles,
                    mock(RepositorioCategorias.class),
                    mock(RepositorioDonaciones.class),
                    mock(TransactionTemplate.class),
                    validadorAdmin);
        }

        @Test
        @DisplayName("actualizar un perfil verifica los permisos del administrador")
        void actualizarVerificaPermisos() {
            UUID idUsuario = UUID.randomUUID();
            when(repoPerfiles.findById(idUsuario))
                    .thenReturn(Optional.of(new Perfil(idUsuario, "Ana")));
            when(repoPerfiles.save(any(Perfil.class)))
                    .thenAnswer(invoc -> invoc.getArgument(0));

            perfilService.actualizarDatosPerfil(
                    idUsuario, ADMIN, new PerfilDTO("Ana2", null, null, null));

            verify(validadorAdmin).verificarPermisos(ADMIN);
        }

        @Test
        @DisplayName("actualizar un perfil NO guarda si el administrador no tiene permisos")
        void actualizarNoGuardaSiNoTienePermisos() {
            UUID idUsuario = UUID.randomUUID();
            Mockito.doThrow(new SecurityException("no es admin"))
                   .when(validadorAdmin).verificarPermisos(ADMIN);

            assertThatThrownBy(() -> perfilService.actualizarDatosPerfil(
                    idUsuario, ADMIN, new PerfilDTO("Ana2", null, null, null)))
                    .isInstanceOf(SecurityException.class);

            verify(repoPerfiles, never()).save(any());
        }

        @Test
        @DisplayName("borrar un perfil verifica los permisos del administrador")
        void borrarVerificaPermisos() {
            UUID idUsuario = UUID.randomUUID();
            when(repoPerfiles.existsById(idUsuario)).thenReturn(true);

            perfilService.eliminarPerfil(idUsuario, ADMIN);

            verify(validadorAdmin).verificarPermisos(ADMIN);
        }

        @Test
        @DisplayName("borrar un perfil NO borra si el administrador no tiene permisos")
        void borrarNoBorraSiNoTienePermisos() {
            UUID idUsuario = UUID.randomUUID();
            Mockito.doThrow(new SecurityException("no es admin"))
                   .when(validadorAdmin).verificarPermisos(ADMIN);

            assertThatThrownBy(() -> perfilService.eliminarPerfil(idUsuario, ADMIN))
                    .isInstanceOf(SecurityException.class);

            verify(repoPerfiles, never()).deleteById(any());
        }
    }
}

