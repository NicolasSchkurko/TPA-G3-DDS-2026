package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.gestores.GestorPublicacionEventos;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioDirecciones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEntidades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioPaises;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioProvincias;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioUnidadesDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un DELETE que borra bien tiene que decir que borró.
 *
 * <p><b>Los tres servicios tenían el mismo bug, copiado.</b> El {@code if (isPresent())} decidía
 * mal: borraban adentro y tiraban la excepción después, así que respondían 404 "no encontrado"
 * justo cuando el recurso existía y el borrado funcionó. Y cuando no existía, no entraban al if
 * y devolvían 204 en silencio, que es el error al revés.
 *
 * <p><b>Por qué 404 después de borrar es peor que un error visible.</b> Un cliente que reintenta
 * el DELETE lee 404 y da por hecho que el recurso ya no está, cuando en realidad lo acaba de
 * eliminar. No hay forma de distinguir los dos casos desde afuera.
 *
 * <p>Los repositorios van mockeados: lo que se prueba es la <i>decisión</i> del service —borrar
 * y no tirar, o tirar sin borrar— que es lo que estaba invertido.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Los tres DELETE devuelven 204 cuando borran y 404 solo cuando no havia nada que borrar")
class DeleteDevuelve204Test {

    private static final UUID ID_DONACION =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ID_CHOFER =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String PATENTE = "AB123CD";

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioRutas repoRutas;
    @Mock private RepositorioEntidades repoEntidades;
    @Mock private RepositorioDirecciones repoDirecciones;
    @Mock private RepositorioCiudades repoCiudades;
    @Mock private RepositorioProvincias repoProvincias;
    @Mock private RepositorioPaises repoPaises;
    @Mock private RepositorioUnidadesDeMedida repoUnidades;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;
    @Mock private PlatformTransactionManager gestorTransacciones;

    @Mock private RepositorioCamiones repoCamiones;
    @Mock private RepositorioChoferes repoChoferes;
    @Mock private GestorCamiones gestorCamiones;

    @InjectMocks private EntregaService entregaService;
    @InjectMocks private CamionService camionService;
    @InjectMocks private ChoferService choferService;

    private static ItemEntrega itemExistente() {
        return new ItemEntrega(ID_DONACION, 1, UnidadDeMedida.UNIDADES, null);
    }

    private static Camion camionExistente() {
        return new Camion(PATENTE, 10.0, 3.0, 1000.0, true);
    }

    private static Chofer choferExistente() {
        return new Chofer(ID_CHOFER, "Juan Perez", true);
    }

    // --- Entregas ---

    @Test
    @DisplayName("Borrar una entrega que existe no tira excepcion: eso era el 404 falso")
    void borrarEntregaExistenteNoTira() {
        // Esto es lo que pasaba: el throw estaba adentro del if (isPresent()), o sea se lanzaba
        // cuando el item existia. El DELETE borraba bien y el controller respondia 404.
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(itemExistente()));

        assertThatCode(() -> entregaService.delete(ID_DONACION)).doesNotThrowAnyException();

        verify(repoItemEntrega).deleteById(ID_DONACION);
    }

    @Test
    @DisplayName("Borrar una entrega que no existe tira y NO intenta borrar")
    void borrarEntregaInexistenteTira() {
        // El otro caso, que estaba al reves: no se entraba al if, no se borraba y no se tiraba
        // nada, asi que el endpoint respondia 204 en silencio para un recurso inexistente.
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entregaService.delete(ID_DONACION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entrega no encontrada");

        verify(repoItemEntrega, never()).deleteById(ID_DONACION);
    }

    // --- Camiones ---

    @Test
    @DisplayName("Borrar un camion que existe no tira excepcion")
    void borrarCamionExistenteNoTira() {
        when(repoCamiones.findById(PATENTE)).thenReturn(Optional.of(camionExistente()));

        assertThatCode(() -> camionService.delete(PATENTE)).doesNotThrowAnyException();

        verify(repoCamiones).deleteById(PATENTE);
    }

    @Test
    @DisplayName("Borrar un camion que no existe tira y NO intenta borrar")
    void borrarCamionInexistenteTira() {
        when(repoCamiones.findById(PATENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> camionService.delete(PATENTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Camión no encontrado");

        verify(repoCamiones, never()).deleteById(PATENTE);
    }

    // --- Choferes ---

    @Test
    @DisplayName("Borrar un chofer que existe no tira excepcion")
    void borrarChoferExistenteNoTira() {
        when(repoChoferes.findById(ID_CHOFER)).thenReturn(Optional.of(choferExistente()));

        assertThatCode(() -> choferService.delete(ID_CHOFER)).doesNotThrowAnyException();

        verify(repoChoferes).deleteById(ID_CHOFER);
    }

    @Test
    @DisplayName("Borrar un chofer que no existe tira y NO intenta borrar")
    void borrarChoferInexistenteTira() {
        when(repoChoferes.findById(ID_CHOFER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> choferService.delete(ID_CHOFER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chofer no encontrado");

        verify(repoChoferes, never()).deleteById(ID_CHOFER);
    }
}