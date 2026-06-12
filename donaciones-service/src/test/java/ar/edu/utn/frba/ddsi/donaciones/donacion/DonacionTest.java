package ar.edu.utn.frba.ddsi.donaciones.donacion;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DonacionTest {

    @Test
    @DisplayName("Crear y verificar los valores")
    public void crearDonacion() {
        // Arrange
        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien subcategoria = new SubcategoriaBien("Enlatados", alimentos);
        BienConEstado bien1 = new BienConEstado("Lentejas", subcategoria, 5, UnidadDeMedida.KILOGRAMOS, false);
        BienConEstado bien2 = new BienConEstado("Atun", subcategoria, 3, UnidadDeMedida.KILOGRAMOS, false);
        LocalDate fechaEntrega = LocalDate.of(2026, 12, 31);

        Donacion donacion = new Donacion(
            null,
            null,
            "Donacion de prueba",
            List.of(bien1, bien2),
            Estado.EN_DEPOSITO,
            subcategoria,
            fechaEntrega
        );

        // Assert
        assertEquals("Donacion de prueba", donacion.getDescripcion());
        assertEquals(Estado.EN_DEPOSITO, donacion.getEstado());
        assertEquals(subcategoria, donacion.getSubcategoria());
        assertEquals(fechaEntrega, donacion.getFechaEntrega());
        assertEquals(2, donacion.getBienes().size());
        assertTrue(donacion.getBienes().contains(bien1));
        assertTrue(donacion.getBienes().contains(bien2));
    }
    
    @Test
    @DisplayName("Retorna la suma total de bienes dentro de la donacion")
    public void sumaCantidadBienes() {
        // Arrange
        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien subcategoria = new SubcategoriaBien("Enlatados", alimentos);

        BienConEstado bien1 = new BienConEstado("Lentejas", subcategoria, 5, UnidadDeMedida.KILOGRAMOS, false);
        BienConEstado bien2 = new BienConEstado("Garbanzo", subcategoria, 3, UnidadDeMedida.KILOGRAMOS, false);

        Donacion donacion = new Donacion(
            null,
            null,
            "Donacion de prueba",
            List.of(bien1, bien2),
            Estado.EN_DEPOSITO,
            subcategoria,
            LocalDate.of(2026, 12, 31)
        );

        // Assert
        assertEquals(8, donacion.sumaCantidadBienes());
    }
}