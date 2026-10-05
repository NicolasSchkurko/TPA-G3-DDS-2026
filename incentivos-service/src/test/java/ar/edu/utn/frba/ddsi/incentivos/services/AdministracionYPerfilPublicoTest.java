package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.RankingMesDTO;
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
import ar.edu.utn.frba.ddsi.incentivos.clients.DonacionClient;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Las escrituras de administración tienen que exigir administrador (punto 21) y el perfil
 * tiene que poder consultarse públicamente (punto 8).
 */
@DisplayName("Puntos 21 y 8: control de admin y vista pública")
class AdministracionYPerfilPublicoTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID OTRO = UUID.randomUUID();

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
                    mock(DonacionClient.class));
        }

        @Test
        @DisplayName("devuelve el nombre de usuario y el de su categoría")
        void devuelveNombreYCategoria() {
            UUID idUsuario = UUID.randomUUID();
            Categoria categoria = new Categoria("Transformador", null, 1, List.of());

            Perfil perfil = new Perfil(idUsuario, "Ana");
            perfil.setCategoriaActual(categoria);
            when(repoPerfiles.findByIdUsuario(idUsuario)).thenReturn(Optional.of(perfil));

            PerfilPublicoDTO dto = perfilService.obtenerPerfilPublico(idUsuario);

            assertThat(dto.getNombreUsuario()).isEqualTo("Ana");
            assertThat(dto.getNombreCategoria()).isEqualTo("Transformador");
        }

        @Test
        @DisplayName("un perfil sin categoría responde igual, con la categoría en null")
        void sinCategoriaRespondeConNull() {
            UUID idUsuario = UUID.randomUUID();
            Perfil perfil = new Perfil(idUsuario, "Beto");
            when(repoPerfiles.findByIdUsuario(idUsuario)).thenReturn(Optional.of(perfil));

            PerfilPublicoDTO dto = perfilService.obtenerPerfilPublico(idUsuario);

            // El donante existe, asi que no es un 404: su nombre tiene que poder verse.
            assertThat(dto.getNombreUsuario()).isEqualTo("Beto");
            assertThat(dto.getNombreCategoria()).isNull();
        }

        @Test
        @DisplayName("si el donante no existe responde 404")
        void siNoExisteResponde404() {
            UUID idUsuario = UUID.randomUUID();
            when(repoPerfiles.findByIdUsuario(idUsuario)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> perfilService.obtenerPerfilPublico(idUsuario))
                    .isInstanceOf(InexistenteException.class);
        }
    }
}

