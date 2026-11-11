package ar.edu.utn.frba.ddsi.donaciones.direccion;

import ar.edu.utn.frba.ddsi.donaciones.dto.direccion.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Punto 75: las coordenadas viajan en el DTO de ida y de vuelta. */
class DireccionDTOTest {

    private final Pais pais = new Pais("Argentina");
    private final Provincia provincia = new Provincia("Buenos Aires", pais);
    private final Ciudad ciudad = new Ciudad("Buenos Aires", provincia);

    @Test
    @DisplayName("from() copia las coordenadas de la entidad")
    void fromCopiaCoordenadas() {
        Direccion direccion = new Direccion("Av. Medrano", "Lavalle", 951, 2, "A", ciudad);
        direccion.setLatitud(-34.6037);
        direccion.setLongitud(-58.3816);

        DireccionDTO dto = DireccionDTO.from(direccion);

        assertEquals(-34.6037, dto.getLatitud());
        assertEquals(-58.3816, dto.getLongitud());
        assertTrue(dto.tieneCoordenadas());
    }

    @Test
    @DisplayName("toDomain() reconstruye las coordenadas del request")
    void toDomainReconstruyeCoordenadas() {
        DireccionDTO dto = new DireccionDTO();
        dto.setCalleUno("Av. Medrano");
        dto.setAltura(951);
        dto.setPiso(2);
        dto.setDepartamento("A");
        dto.setCiudad("Buenos Aires");
        dto.setProvincia("Buenos Aires");
        dto.setPais("Argentina");
        dto.setLatitud(-34.6037);
        dto.setLongitud(-58.3816);

        Direccion direccion = dto.toDomain();

        assertTrue(direccion.tieneCoordenadas());
        assertEquals(-34.6037, direccion.getLatitud());
        assertEquals(-58.3816, direccion.getLongitud());
    }

    @Test
    @DisplayName("toDomain(Ciudad) también conserva las coordenadas")
    void toDomainConCiudadConservaCoordenadas() {
        DireccionDTO dto = new DireccionDTO();
        dto.setCalleUno("Av. Medrano");
        dto.setAltura(951);
        dto.setLatitud(-34.6037);
        dto.setLongitud(-58.3816);

        Direccion direccion = dto.toDomain(ciudad);

        assertTrue(direccion.tieneCoordenadas());
        assertEquals(ciudad, direccion.getCiudad());
        assertEquals(0, direccion.getPiso());
    }

    @Test
    @DisplayName("Un piso null en el request no rompe toDomain(): se guarda como 0")
    void pisoNuloSeGuardaComoCero() {
        DireccionDTO dto = new DireccionDTO();
        dto.setCalleUno("Av. Medrano");
        dto.setAltura(951);

        Direccion sinCiudad = dto.toDomain();
        Direccion conCiudad = dto.toDomain(ciudad);

        assertEquals(0, sinCiudad.getPiso());
        assertEquals(0, conCiudad.getPiso());
    }

    @Test
    @DisplayName("Sin coordenadas el DTO lo informa")
    void sinCoordenadas() {
        DireccionDTO dto = DireccionDTO.from(new Direccion("Av. Medrano", null, 951, 0, null, ciudad));

        assertFalse(dto.tieneCoordenadas());
    }

    @Test
    @DisplayName("fromSinCoordenadas() no publica la ubicación del donante")
    void fromSinCoordenadasOcultaLaUbicacion() {
        Direccion direccion = new Direccion("Av. Medrano", null, 951, 0, null, ciudad);
        direccion.setLatitud(-34.6037);
        direccion.setLongitud(-58.3816);

        DireccionDTO dto = DireccionDTO.fromSinCoordenadas(direccion);

        assertEquals("Av. Medrano", dto.getCalleUno());
        assertFalse(dto.tieneCoordenadas());
    }
}
