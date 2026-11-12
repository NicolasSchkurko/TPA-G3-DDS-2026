package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.camion.CamionDTO;
import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CamionService")
class CamionServiceTest {

    @Mock private RepositorioCamiones repoCamiones;
    @Mock private RepositorioChoferes repoChoferes;

    private CamionService service;

    @BeforeEach
    void setUp() {
        service = new CamionService(new GestorCamiones(), repoCamiones, repoChoferes);
    }

    // --- update: chofer ---

    @Test
    @DisplayName("Sin idChofer en el body, el chofer asignado se conserva")
    void sinIdChoferConservaElChofer() {
        Chofer chofer = new Chofer(UUID.randomUUID(), "Ana");
        Camion camion = new Camion(chofer, "AAA111", 10.0, 3.0, 1000.0, true);
        when(repoCamiones.findById("AAA111")).thenReturn(Optional.of(camion));

        CamionDTO dto = new CamionDTO();
        dto.setDisponible(false);
        dto.setCapacidadCarga(2000.0);

        CamionDTO resultado = service.update("AAA111", dto);

        assertThat(camion.getChofer()).isNotNull();
        assertThat(camion.getChofer().getIdChofer()).isEqualTo(chofer.getIdChofer());
        assertThat(resultado.getIdChofer()).isEqualTo(chofer.getIdChofer());
        assertThat(resultado.getDisponible()).isFalse();
        assertThat(resultado.getCapacidadCarga()).isEqualTo(2000.0);
    }

    @Test
    @DisplayName("Con idChofer válido, el chofer sí se actualiza")
    void conIdChoferActualizaElChofer() {
        Chofer viejo = new Chofer(UUID.randomUUID(), "Viejo");
        Chofer nuevo = new Chofer(UUID.randomUUID(), "Nuevo");
        Camion camion = new Camion(viejo, "BBB222", 10.0, 3.0, 1000.0, true);
        when(repoCamiones.findById("BBB222")).thenReturn(Optional.of(camion));
        when(repoChoferes.findById(nuevo.getIdChofer())).thenReturn(Optional.of(nuevo));

        CamionDTO dto = new CamionDTO();
        dto.setIdChofer(nuevo.getIdChofer());

        CamionDTO resultado = service.update("BBB222", dto);

        assertThat(camion.getChofer().getIdChofer()).isEqualTo(nuevo.getIdChofer());
        assertThat(resultado.getIdChofer()).isEqualTo(nuevo.getIdChofer());
    }

    @Test
    @DisplayName("Con idChofer inexistente responde recurso no encontrado y no toca el camión")
    void conIdChoferInexistenteFalla() {
        Chofer viejo = new Chofer(UUID.randomUUID(), "Viejo");
        Camion camion = new Camion(viejo, "CCC333", 10.0, 3.0, 1000.0, true);

        UUID inexistente = UUID.randomUUID();
        when(repoChoferes.findById(inexistente)).thenReturn(Optional.empty());

        CamionDTO dto = new CamionDTO();
        dto.setIdChofer(inexistente);

        assertThatThrownBy(() -> service.update("CCC333", dto))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(camion.getChofer().getIdChofer()).isEqualTo(viejo.getIdChofer());
        verify(repoCamiones, never()).save(any());
    }

    @Test
    @DisplayName("Con idChofer inactivo responde recurso no encontrado")
    void conIdChoferInactivoFalla() {
        Chofer inactivo = new Chofer(UUID.randomUUID(), "Baja");
        inactivo.darDeBaja();
        when(repoChoferes.findById(inactivo.getIdChofer())).thenReturn(Optional.of(inactivo));

        CamionDTO dto = new CamionDTO();
        dto.setIdChofer(inactivo.getIdChofer());

        assertThatThrownBy(() -> service.update("DDD444", dto))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(repoCamiones, never()).save(any());
    }

    @Test
    @DisplayName("Con patente inexistente responde recurso no encontrado y no guarda")
    void conPatenteInexistenteFalla() {
        when(repoCamiones.findById("ZZZ999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("ZZZ999", new CamionDTO()))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Camión no encontrado");
        verify(repoCamiones, never()).save(any());
    }

    // --- delete ---

    @Test
    @DisplayName("Borrar un camión que existe no tira excepción")
    void borrarCamionExistenteNoTira() {
        when(repoCamiones.findById("AB123CD"))
                .thenReturn(Optional.of(new Camion("AB123CD", 10.0, 3.0, 1000.0, true)));

        assertThatCode(() -> service.delete("AB123CD")).doesNotThrowAnyException();

        verify(repoCamiones).deleteById("AB123CD");
    }

    @Test
    @DisplayName("Borrar un camión que no existe tira y NO intenta borrar")
    void borrarCamionInexistenteTira() {
        when(repoCamiones.findById("AB123CD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("AB123CD"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Camión no encontrado");

        verify(repoCamiones, never()).deleteById("AB123CD");
    }

    // --- paginación ---

    @Test
    @DisplayName("Se pasa el Pageable al repositorio y se devuelve la página")
    void sePaginaEnElRepositorio() {
        Pageable pageable = PageRequest.of(1, 2);
        Camion camion = new Camion("AAA111", 10.0, 3.0, 1000.0, true);
        when(repoCamiones.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(camion), pageable, 5));

        Page<CamionDTO> resultado = service.findAll(pageable);

        verify(repoCamiones).findAll(pageable);
        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getContent()).extracting(CamionDTO::getPatente)
                .containsExactly("AAA111");
    }
}
