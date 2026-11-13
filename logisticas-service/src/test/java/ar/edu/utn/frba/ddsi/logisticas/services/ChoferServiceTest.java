package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.chofer.ChoferDTO;
import ar.edu.utn.frba.ddsi.logisticas.exceptions.RecursoNoEncontradoException;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorChoferes;
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
@DisplayName("ChoferService")
class ChoferServiceTest {

    private static final UUID ID_CHOFER =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String PATENTE = "AB123CD";

    @Mock private RepositorioChoferes repoChoferes;
    @Mock private RepositorioCamiones repoCamiones;

    private ChoferService service;

    @BeforeEach
    void setUp() {
        service = new ChoferService(repoChoferes, new GestorChoferes(repoChoferes, repoCamiones));
    }

    // --- delete ---

    @Test
    @DisplayName("Borrar un chofer que existe aplica baja lógica, desvincula camión y no tira excepción")
    void borrarChoferExistenteNoTira() {
        Chofer chofer = new Chofer(ID_CHOFER, "Juan Perez", true);
        Camion camion = new Camion(PATENTE, 10.0, 3.0, 1000.0, true);
        camion.setChofer(chofer);

        when(repoChoferes.findById(ID_CHOFER)).thenReturn(Optional.of(chofer));
        when(repoCamiones.findByChofer_IdChofer(ID_CHOFER)).thenReturn(Optional.of(camion));

        assertThatCode(() -> service.delete(ID_CHOFER)).doesNotThrowAnyException();

        assertThat(chofer.isActivo()).isFalse();
        assertThat(chofer.isDisponible()).isFalse();
        assertThat(chofer.getFechaBaja()).isNotNull();
        assertThat(camion.getChofer()).isNull();

        verify(repoChoferes).save(chofer);
        verify(repoCamiones).save(camion);
        verify(repoChoferes, never()).deleteById(any());
    }

    @Test
    @DisplayName("Borrar un chofer que no existe tira y NO intenta borrar")
    void borrarChoferInexistenteTira() {
        when(repoChoferes.findById(ID_CHOFER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(ID_CHOFER))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Chofer no encontrado");

        verify(repoChoferes, never()).deleteById(ID_CHOFER);
    }

    @Test
    @DisplayName("Borrar un chofer ya dado de baja tira Chofer no encontrado")
    void borrarChoferYaInactivoTira() {
        Chofer chofer = new Chofer(ID_CHOFER, "Juan Perez", true);
        chofer.darDeBaja();

        when(repoChoferes.findById(ID_CHOFER)).thenReturn(Optional.of(chofer));

        assertThatThrownBy(() -> service.delete(ID_CHOFER))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("Chofer no encontrado");

        verify(repoChoferes, never()).deleteById(any());
    }

    @Test
    @DisplayName("El alta y la edición conservan el email del chofer")
    void altaYEdicionConservanEmail() {
        ChoferDTO alta = new ChoferDTO();
        alta.setNombre("Chofer Mail");
        alta.setEmail("chofer@ddsi.local");

        ChoferDTO creado = service.create(alta);
        assertThat(creado.getEmail()).isEqualTo("chofer@ddsi.local");

        Chofer existente = new Chofer(ID_CHOFER, "Chofer Mail", true);
        existente.setEmail("viejo@ddsi.local");
        when(repoChoferes.findById(ID_CHOFER)).thenReturn(Optional.of(existente));

        ChoferDTO cambio = new ChoferDTO();
        cambio.setNombre("Chofer Mail");
        cambio.setEmail("nuevo@ddsi.local");
        ChoferDTO actualizado = service.update(ID_CHOFER, cambio);

        assertThat(actualizado.getEmail()).isEqualTo("nuevo@ddsi.local");
    }

    // --- paginación ---

    @Test
    @DisplayName("Se pagina con el filtro de activos en la query, no en memoria")
    void sePaginaConFiltroDeActivosEnLaQuery() {
        Pageable pageable = PageRequest.of(0, 2);
        Chofer chofer = new Chofer(UUID.randomUUID(), "Ana");
        when(repoChoferes.findActivos(pageable))
                .thenReturn(new PageImpl<>(List.of(chofer), pageable, 1));

        Page<ChoferDTO> resultado = service.findAll(pageable);

        verify(repoChoferes).findActivos(pageable);
        verify(repoChoferes, never()).findAll();
        assertThat(resultado.getContent()).hasSize(1);
    }
}
