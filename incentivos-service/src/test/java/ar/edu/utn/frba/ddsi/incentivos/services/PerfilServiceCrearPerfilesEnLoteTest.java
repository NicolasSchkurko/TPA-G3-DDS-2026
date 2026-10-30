package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.PerfilDonanteDTO;
import ar.edu.utn.frba.ddsi.incentivos.dto.Persona.ResultadoLotePerfilesDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.CategoriaPerfil.Categoria;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Perfil;
import ar.edu.utn.frba.ddsi.incentivos.models.gestores.ValidadorAdmin;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioCategorias;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPerfiles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Alta en lote de perfiles: la usan las importaciones de donaciones. Tiene que ser idempotente
 * (los perfiles existentes se saltean) y aislar las filas rotas: una falla no tumba el lote.
 */
class PerfilServiceCrearPerfilesEnLoteTest {

    private PerfilDonanteDTO perfil(UUID id, String nombre) {
        return new PerfilDonanteDTO(id, nombre, "DONANTE");
    }

    private TransactionTemplate fakeTemplate() {
        return new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
    }

    @Test
    @DisplayName("Crea los nuevos, saltea los existentes y reporta los rotos sin abortar el lote")
    void creaSalteaYAislaErrores() {
        RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);

        PerfilService service = new PerfilService(
                repoPerfiles,
                repoCategorias,
                mock(RepositorioDonaciones.class),
                fakeTemplate(),
                mock(ValidadorAdmin.class));

        UUID nuevo1 = UUID.randomUUID();
        UUID nuevo2 = UUID.randomUUID();
        UUID existente = UUID.randomUUID();
        UUID roto = UUID.randomUUID();

        when(repoPerfiles.existsById(nuevo1)).thenReturn(false);
        when(repoPerfiles.existsById(nuevo2)).thenReturn(false);
        when(repoPerfiles.existsById(existente)).thenReturn(true);
        when(repoPerfiles.existsById(roto)).thenThrow(new RuntimeException("la base se cayó"));
        when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.of(mock(Categoria.class)));
        when(repoPerfiles.save(any(Perfil.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ResultadoLotePerfilesDTO resultado = service.crearPerfilesEnLote(List.of(
                perfil(nuevo1, "Sofia"),
                perfil(existente, "Ana"),
                perfil(nuevo2, "Benjamín"),
                perfil(roto, "Lucía")));

        assertThat(resultado.getCreados()).isEqualTo(2);
        assertThat(resultado.getYaExistian()).isEqualTo(1);
        assertThat(resultado.getErrores()).hasSize(1);
        assertThat(resultado.getErrores().get(0))
                .contains(roto.toString())
                .contains("la base se cayó");
    }

    @Test
    @DisplayName("Reintentar un lote entero ya creado no es un error: todo queda como yaExistian")
    void reintentoCompletoEsIdempotente() {
        RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        when(repoPerfiles.existsById(any())).thenReturn(true);

        PerfilService service = new PerfilService(
                repoPerfiles,
                mock(RepositorioCategorias.class),
                mock(RepositorioDonaciones.class),
                fakeTemplate(),
                mock(ValidadorAdmin.class));

        ResultadoLotePerfilesDTO resultado = service.crearPerfilesEnLote(List.of(
                perfil(UUID.randomUUID(), "Sofia"),
                perfil(UUID.randomUUID(), "Ana")));

        assertThat(resultado.getCreados()).isZero();
        assertThat(resultado.getYaExistian()).isEqualTo(2);
        assertThat(resultado.getErrores()).isEmpty();
    }

    @Test
    @DisplayName("Cada perfil se crea dentro de su propia transacción independiente")
    void cadaItemUsaSuPropiaTransaccion() {
        RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);
        AtomicInteger transacciones = new AtomicInteger();

        TransactionTemplate template = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                transacciones.incrementAndGet();
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };

        PerfilService service = new PerfilService(
                repoPerfiles,
                repoCategorias,
                mock(RepositorioDonaciones.class),
                template,
                mock(ValidadorAdmin.class));

        when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.of(mock(Categoria.class)));
        when(repoPerfiles.save(any(Perfil.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.crearPerfilesEnLote(List.of(
                perfil(UUID.randomUUID(), "Sofia"),
                perfil(UUID.randomUUID(), "Ana")));

        assertThat(transacciones.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cuando un ítem lanza DataIntegrityViolationException, no se aborta el lote y se formatea el error")
    void errorDeIntegridadNoTumbaElLoteYSeFormatea() {
        RepositorioPerfiles repoPerfiles = mock(RepositorioPerfiles.class);
        RepositorioCategorias repoCategorias = mock(RepositorioCategorias.class);

        PerfilService service = new PerfilService(
                repoPerfiles,
                repoCategorias,
                mock(RepositorioDonaciones.class),
                fakeTemplate(),
                mock(ValidadorAdmin.class));

        UUID nuevo = UUID.randomUUID();
        UUID roto = UUID.randomUUID();

        when(repoPerfiles.existsById(any())).thenReturn(false);
        when(repoCategorias.obtenerCategoriaBase()).thenReturn(Optional.of(mock(Categoria.class)));
        when(repoPerfiles.save(any(Perfil.class))).thenAnswer(invocacion -> {
            Perfil p = invocacion.getArgument(0);
            if (p.getIdUsuario().equals(roto)) {
                throw new org.springframework.dao.DataIntegrityViolationException("violación de constraint");
            }
            return p;
        });

        ResultadoLotePerfilesDTO resultado = service.crearPerfilesEnLote(List.of(
                perfil(nuevo, "Sofia"),
                perfil(roto, "Lucía")));

        assertThat(resultado.getCreados()).isEqualTo(1);
        assertThat(resultado.getYaExistian()).isZero();
        assertThat(resultado.getErrores()).hasSize(1);
        assertThat(resultado.getErrores().get(0))
                .contains(roto.toString())
                .contains("Error de integridad de datos al persistir el perfil");
    }
}
