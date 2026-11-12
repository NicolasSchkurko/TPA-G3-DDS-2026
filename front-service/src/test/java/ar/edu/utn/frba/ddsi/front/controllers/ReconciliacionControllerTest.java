package ar.edu.utn.frba.ddsi.front.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.utn.frba.ddsi.front.clients.DonacionesClient;
import ar.edu.utn.frba.ddsi.front.clients.IncentivosClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("Reconciliación puntual desde el front")
class ReconciliacionControllerTest {

    private final DonacionesClient donaciones = mock(DonacionesClient.class);
    private final IncentivosClient incentivos = mock(IncentivosClient.class);
    private final ReconciliacionController controller =
            new ReconciliacionController(donaciones, incentivos);

    @Test
    @DisplayName("si el donante sigue activo no toca incentivos")
    void donanteActivoNoHaceNada() {
        UUID id = UUID.randomUUID();
        when(donaciones.donanteSigueActivo(id)).thenReturn(Optional.of(true));

        var respuesta = controller.reconciliar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(incentivos, never()).eliminarPerfil(id);
    }

    @Test
    @DisplayName("si el donante no existe o está inactivo da de baja el perfil")
    void donanteInactivoDaDeBajaElPerfil() {
        UUID id = UUID.randomUUID();
        when(donaciones.donanteSigueActivo(id)).thenReturn(Optional.of(false));

        var respuesta = controller.reconciliar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(incentivos).eliminarPerfil(id);
    }

    @Test
    @DisplayName("si no se pudo verificar el donante no lo da de baja")
    void donanteNoVerificadoNoDaDeBaja() {
        UUID id = UUID.randomUUID();
        when(donaciones.donanteSigueActivo(id)).thenReturn(Optional.empty());

        var respuesta = controller.reconciliar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        verify(incentivos, never()).eliminarPerfil(id);
    }

    @Test
    @DisplayName("si incentivos falla responde 502 sin propagar la excepción")
    void incentivosCaidoRespondeBadGateway() {
        UUID id = UUID.randomUUID();
        when(donaciones.donanteSigueActivo(id)).thenReturn(Optional.of(false));
        doThrow(new RuntimeException("incentivos caído")).when(incentivos).eliminarPerfil(id);

        var respuesta = controller.reconciliar(id);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
