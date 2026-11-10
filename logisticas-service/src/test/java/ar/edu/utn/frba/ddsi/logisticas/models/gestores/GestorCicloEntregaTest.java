package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GestorCicloEntrega")
class GestorCicloEntregaTest {

    private static final UUID ID_DONACION = UUID.randomUUID();

    @Mock private RepositorioItemEntrega repoItemEntrega;
    @Mock private RepositorioRutas repoRutas;
    @Mock private GestorPublicacionEventos gestorPublicacionEventos;

    private GestorCicloEntrega gestor() {
        return new GestorCicloEntrega(repoItemEntrega, repoRutas, gestorPublicacionEventos,
                new GestorRutas(repoRutas, repoItemEntrega));
    }

    private ItemEntrega item(EstadoEntrega estado) {
        ItemEntrega item = new ItemEntrega(ID_DONACION, 1, UnidadDeMedida.UNIDADES, null);
        item.setEstado(estado);
        when(repoItemEntrega.findById(ID_DONACION)).thenReturn(Optional.of(item));
        return item;
    }

    private Ruta rutaCon(ItemEntrega item) {
        Ruta ruta = new Ruta(null);
        Parada parada = new Parada();
        parada.getItems().add(item);
        ruta.getParadas().add(parada);
        when(repoRutas.findFirstByParadasItemsIdDonacion(ID_DONACION)).thenReturn(Optional.of(ruta));
        return ruta;
    }

    @Test
    @DisplayName("Reingreso falla si el ítem está ENTREGADA")
    void reingresoFallaSiEntregada() {
        item(EstadoEntrega.ENTREGADA);

        assertThatThrownBy(() -> gestor().reingresar(ID_DONACION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NO_RECIBIDA");

        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
    }

    @Test
    @DisplayName("Reingreso falla si el ítem está EN_CAMINO")
    void reingresoFallaSiEnCamino() {
        item(EstadoEntrega.EN_CAMINO);

        assertThatThrownBy(() -> gestor().reingresar(ID_DONACION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NO_RECIBIDA");

        verify(gestorPublicacionEventos, never()).publicarReingresoDeposito(any());
    }

    @Test
    @DisplayName("Reingreso exitoso si el ítem está NO_RECIBIDA")
    void reingresoExitosoSiNoRecibida() {
        ItemEntrega item = item(EstadoEntrega.NO_RECIBIDA);
        when(gestorPublicacionEventos.publicarReingresoDeposito(item)).thenReturn(item);

        gestor().reingresar(ID_DONACION);

        verify(gestorPublicacionEventos).publicarReingresoDeposito(item);
        verify(repoItemEntrega, atLeastOnce()).save(item);
    }

    @Test
    @DisplayName("Confirmar exige foto")
    void confirmarExigeFoto() {
        item(EstadoEntrega.EN_CAMINO);

        assertThatThrownBy(() -> gestor().confirmar(ID_DONACION, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("foto");
    }

    @Test
    @DisplayName("Marcar fallida exige justificación")
    void fallidaExigeJustificacion() {
        item(EstadoEntrega.EN_CAMINO);

        assertThatThrownBy(() -> gestor().marcarFallida(ID_DONACION, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("justificar");
    }

    @Test
    @DisplayName("Confirmar guarda el ítem y sincroniza la parada de la ruta")
    void confirmarSincronizaLaRuta() {
        ItemEntrega item = item(EstadoEntrega.EN_CAMINO);
        ItemEntrega itemDeRuta = new ItemEntrega(ID_DONACION, 1, UnidadDeMedida.UNIDADES, null);
        Ruta ruta = rutaCon(itemDeRuta);
        when(gestorPublicacionEventos.publicarEntregaConfirmada(item, ruta, "http://foto"))
                .thenReturn(item);

        gestor().confirmar(ID_DONACION, "http://foto");

        verify(repoItemEntrega).save(item);
        verify(repoRutas).save(ruta);
        assertThat(itemDeRuta.getEstado()).isEqualTo(item.getEstado());
    }
}
