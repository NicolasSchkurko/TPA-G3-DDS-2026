package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.dto.n8n.PerfilPublicacionDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.PublicacionPendienteN8n;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioPublicacionesPendientes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("PublicacionesN8nService: reclamo con límite de una fila")
class PublicacionesN8nServiceTest {

    private final RepositorioPublicacionesPendientes repositorio =
            mock(RepositorioPublicacionesPendientes.class);
    private final PublicacionesN8nService service =
            new PublicacionesN8nService(repositorio);

    @Test
    @DisplayName("pide una sola publicación para no bloquear toda la outbox")
    void pideUnaSolaPublicacion() {
        when(repositorio.buscarSiguienteParaEnviar(any(), any()))
                .thenReturn(List.of());

        service.reclamarSiguiente(LocalDateTime.now());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarSiguienteParaEnviar(any(), captor.capture());

        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
        assertThat(captor.getValue().getOffset()).isZero();
    }

    @Test
    @DisplayName("descarta la publicación al alcanzar el máximo de intentos")
    void descartaAlAlcanzarElMaximo() {
        UUID id = UUID.randomUUID();
        PublicacionPendienteN8n publicacion = new PublicacionPendienteN8n(
                new PerfilPublicacionDTO("p", "m", "Ana", UUID.randomUUID()),
                LocalDateTime.now());
        ReflectionTestUtils.setField(publicacion, "id", id);
        for (int i = 0; i < 10; i++) {
            publicacion.reclamarHasta(LocalDateTime.now());
        }
        when(repositorio.findById(id)).thenReturn(Optional.of(publicacion));

        service.reintentar(id, LocalDateTime.now(), "fallo");

        assertThat(publicacion.isDescartada()).isTrue();
    }
}
