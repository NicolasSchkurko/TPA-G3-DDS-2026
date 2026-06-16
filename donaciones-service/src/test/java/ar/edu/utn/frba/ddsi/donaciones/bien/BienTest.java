package ar.edu.utn.frba.ddsi.donaciones.bien;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class BienTest {

    @Test
    @DisplayName("Crear un bien con estado")
    public void crearBienConEstado() {
        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien subcategoria = new SubcategoriaBien("Arroz", alimentos);
        BienConEstado bien = new BienConEstado("Arroz blanco", subcategoria, 2, UnidadDeMedida.KILOGRAMOS, false);

        assertEquals("Arroz blanco", bien.getDescripcion());
        assertEquals(subcategoria, bien.getSubcategoria());
        assertEquals(2, bien.getCantidad());
        assertEquals(UnidadDeMedida.KILOGRAMOS, bien.getUnidadUtilizada());
        assertFalse(bien.isUsado());

    }

    @Test
    @DisplayName("Crear un bien perecedero")
    public void crearBienPerecedero() {
        CategoriaBien alimentos = new CategoriaBien("Alimentos");
        SubcategoriaBien subcategoria = new SubcategoriaBien("Enlatados", alimentos);
        LocalDate fechaVencimiento = LocalDate.of(2026, 12, 31);
        BienPerecedero bien = new BienPerecedero("Atun", subcategoria, 1, UnidadDeMedida.KILOGRAMOS, fechaVencimiento);

        assertEquals("Atun", bien.getDescripcion());
        assertEquals(subcategoria, bien.getSubcategoria());
        assertEquals(1, bien.getCantidad());
        assertEquals(UnidadDeMedida.KILOGRAMOS, bien.getUnidadUtilizada());
        assertEquals(LocalDate.of(2026, 12, 31), bien.getFechaVencimiento());

    }

}