package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Admin.CategoriaDTO;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.ConflictoException;
import ar.edu.utn.frba.ddsi.incentivos.exceptions.InexistenteException;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SecuenciaCategoria;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.SincronizacionPerfiles;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioMisiones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CategoriaService")
class CategoriaServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();

    private RepositorioCategorias repoCategorias;
    private RepositorioMisiones repoMisiones;
    private RepositorioPerfiles repoPerfiles;
    private SecuenciaCategoria gestorSecuencia;
    private CategoriaService service;

    @BeforeEach
    void setUp() {
        repoCategorias = mock(RepositorioCategorias.class);
        repoMisiones = mock(RepositorioMisiones.class);
        repoPerfiles = mock(RepositorioPerfiles.class);
        gestorSecuencia = mock(SecuenciaCategoria.class);
        service = new CategoriaService(
                repoCategorias,
                repoMisiones,
                repoPerfiles,
                gestorSecuencia,
                mock(SincronizacionPerfiles.class),
                mock(ValidadorAdmin.class)
        );
    }

    private static CategoriaDTO dto(String nombre, Integer posicion) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre(nombre);
        dto.setPosicionSecuencia(posicion);
        dto.setMisiones(List.of());
        return dto;
    }

    @Nested
    @DisplayName("Borrar una categoría")
    class Borrar {

        private UUID id;
        private Categoria categoria;

        @BeforeEach
        void preparar() {
            id = UUID.randomUUID();
            categoria = new Categoria("Bronce", null, 2, List.of());
            when(repoCategorias.obtenerPorId(id)).thenReturn(categoria);
        }

        @Test
        @DisplayName("no se borra si tiene donantes asignados, y dice cuántos")
        void noSeBorraSiTieneDonantes() {
            when(repoPerfiles.countByCategoriaActual(categoria)).thenReturn(12L);

            assertThatThrownBy(() -> service.eliminarCategoria(ADMIN, id))
                    .isInstanceOf(ConflictoException.class)
                    .hasMessageContaining("12 donante(s)");

            verify(repoCategorias, never()).delete(any());
        }

        @Test
        @DisplayName("no se borra si tiene donantes, y no corrió la secuencia de posiciones")
        void noSeBorraYSequenciaIntacta() {
            when(repoPerfiles.countByCategoriaActual(categoria)).thenReturn(12L);

            assertThatThrownBy(() -> service.eliminarCategoria(ADMIN, id))
                    .isInstanceOf(ConflictoException.class);

            // Antes fallaba por FK después de desplazar las posiciones.
            verify(gestorSecuencia, never()).desplazarParaEliminar(any(), any());
        }

        @Test
        @DisplayName("se borra y cierra el hueco si nadie la usa")
        void seBorraYDejaLaSecuenciaSinHuecos() {
            when(repoPerfiles.countByCategoriaActual(categoria)).thenReturn(0L);

            service.eliminarCategoria(ADMIN, id);

            verify(repoCategorias).delete(categoria);
            verify(gestorSecuencia).desplazarParaEliminar(repoCategorias, 2);
        }

        @Test
        @DisplayName("si no existe responde 404")
        void siNoExisteResponde404() {
            UUID inexistente = UUID.randomUUID();
            when(repoCategorias.obtenerPorId(inexistente)).thenReturn(null);

            assertThatThrownBy(() -> service.eliminarCategoria(ADMIN, inexistente))
                    .isInstanceOf(InexistenteException.class);

            verify(repoCategorias, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Crear una categoría")
    class Crear {

        @Test
        @DisplayName("rechaza una posición que ya está ocupada")
        void rechazaUnaPosicionOcupada() {
            when(repoCategorias.existsByPosicionSecuencia(3)).thenReturn(true);

            assertThatThrownBy(() -> service.agregarCategoria(ADMIN, dto("Plata", 3)))
                    .isInstanceOf(ConflictoException.class)
                    .hasMessageContaining("posición 3");

            verify(repoCategorias, never()).save(any());
        }

        @Test
        @DisplayName("acepta una posición libre y abre el hueco")
        void aceptaUnaPosicionLibre() {
            when(repoCategorias.existsByPosicionSecuencia(3)).thenReturn(false);
            when(repoCategorias.save(any())).thenAnswer(i -> i.getArgument(0));

            service.agregarCategoria(ADMIN, dto("Plata", 3));

            verify(gestorSecuencia).desplazarParaCrear(repoCategorias, 3, gestorSecuencia.posicionMaxima(repoCategorias));
        }
    }
}
