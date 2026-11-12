package ar.edu.utn.frba.ddsi.incentivos.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad.ImpactoDonacion;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

@DisplayName("DestacadasService: límite y datos públicos")
class DestacadasServiceTest {

    private static final int LIMITE_PREDETERMINADO = 30;

    private final RepositorioDonaciones repositorio = mock(RepositorioDonaciones.class);
    private final DestacadasService service =
            new DestacadasService(repositorio, LIMITE_PREDETERMINADO);

    @Test
    @DisplayName("sin límite explícito usa la property")
    void usaElLimitePredeterminado() {
        when(repositorio.buscarDestacadas(any())).thenReturn(List.of());

        service.obtenerDestacadas(null);

        assertThat(limitePedido()).isEqualTo(LIMITE_PREDETERMINADO);
    }

    @Test
    @DisplayName("mapea solo los datos públicos de la donación")
    void mapeaSoloDatosPublicos() {
        UUID idDonacion = UUID.randomUUID();
        LocalDateTime fecha = LocalDateTime.of(2026, 5, 1, 10, 0);
        ImpactoDonacion donacion = new ImpactoDonacion(
                idDonacion, UUID.randomUUID(), "Comedor Los Piletones", 12,
                fecha, "ALIMENTOS", "MERCEARIA", "ENTREGADA");
        when(repositorio.buscarDestacadas(any())).thenReturn(List.of(donacion));

        var resultado = service.obtenerDestacadas(5).get(0);

        assertThat(resultado.idDonacion()).isEqualTo(idDonacion);
        assertThat(resultado.fechaEntrega()).isEqualTo(fecha);
        assertThat(resultado.cantidadBienes()).isEqualTo(12);
        assertThat(resultado.categoria()).isEqualTo("ALIMENTOS");
        assertThat(resultado.subCategoria()).isEqualTo("MERCEARIA");
        assertThat(resultado.entidadBeneficiaria()).isEqualTo("Comedor Los Piletones");
        assertThat(limitePedido()).isEqualTo(5);
    }

    @Test
    @DisplayName("rechaza límites fuera de 1..100")
    void rechazaLimitesFueraDeRango() {
        assertThatThrownBy(() -> service.obtenerDestacadas(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.obtenerDestacadas(101))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private int limitePedido() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repositorio).buscarDestacadas(captor.capture());
        return captor.getValue().getPageSize();
    }
}
