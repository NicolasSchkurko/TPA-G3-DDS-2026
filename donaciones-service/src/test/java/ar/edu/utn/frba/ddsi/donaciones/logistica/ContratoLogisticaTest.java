package ar.edu.utn.frba.ddsi.donaciones.logistica;

import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.PropuestaAsignacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAsignaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorFormulario;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioBienes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDeResultadosMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioFormularios;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioSubcategoriasDeBienes;
import ar.edu.utn.frba.ddsi.donaciones.messaging.ProductorLogistica;
import ar.edu.utn.frba.ddsi.donaciones.services.DonacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato del mensaje que este servicio publica para logística en
 * {@code logistica.integracion.hash} (routing key de donación asignada).
 *
 * <p>El contrato real vive en logisticas-service ({@code dto.entrega.*}); acá se mira con
 * copias campo a campo del lado receptor, serializado tal como lo hace el
 * {@code Jackson2JsonMessageConverter} del productor (punto 27 de PENDIENTES.md: si un lado
 * renombra o deja de mandar un campo, esto lo detecta en el primer test en vez de en el
 * primer incidente).
 *
 * <p>El caso que motivó el punto: {@code EntregaService.resolverEntidad} de logística hace
 * {@code findById(dto.getIdEntidad())}; si la dirección sale sin {@code idEntidad}, el
 * {@code DonacionListener} descarta el mensaje al 100% de las veces.
 */
public class ContratoLogisticaTest {

    // Fiel al Jackson2JsonMessageConverter default: ObjectMapper sin módulos extra.
    private final ObjectMapper mapper = new ObjectMapper();

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // --- Copias mínimas de los DTO de logisticas-service (el lado que consume) ---

    public static class EntregaMirror {
        public BienesMirror donacionResumen;
        public DireccionMirror entidadBeneficiaria;
    }

    public static class BienesMirror {
        public List<UUID> idsDonaciones;
        public List<BienMirror> bienes;
    }

    public static class BienMirror {
        @NotNull public Integer cantidad;
        @NotBlank public String unidadDeMedida;
        public String estado;
        public Object fechaCambioEstado;
        public String fotoComprobante;
        public DireccionMirror entidadDestino;
        public List<Object> eventos;
    }

    public static class DireccionMirror {
        /** Lo que logística usa para findById: sin él descarta la entrega. */
        @NotNull public UUID idEntidad;
        @NotBlank public String calleUno;
        public String calleDos;
        public Integer altura;
        public Integer piso;
        public String departamento;
        @NotBlank public String ciudad;
        @NotBlank public String provincia;
        @NotBlank public String pais;
    }

    @Test
    @DisplayName("La entrega publicada cumple el contrato que logística consume: sin campos en null")
    void entregaPublicada_cumpleContratoDeLogistica() throws Exception {
        UUID donacionId = UUID.randomUUID();

        Bien bien = mock(Bien.class);
        when(bien.getPeso()).thenReturn(10);
        when(bien.getUnidadUtilizada()).thenReturn(UnidadDeMedida.KILOGRAMOS);

        Ciudad ciudad = new Ciudad("CABA", new Provincia("Buenos Aires", new Pais("Argentina")));
        Direccion direccion = new Direccion("Av Siempreviva 742", null, 742, 0, null, ciudad);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria(direccion, mock(Juridica.class));

        Donacion donacion = new Donacion();
        donacion.setId(donacionId);
        donacion.setEntidad(entidad);

        Bien bienDos = mock(Bien.class);
        when(bienDos.getPeso()).thenReturn(6);
        when(bienDos.getUnidadUtilizada()).thenReturn(UnidadDeMedida.KILOGRAMOS);
        donacion.setBienes(new ArrayList<>(List.of(bien, bienDos)));

        RepositorioDonaciones repoDonaciones = mock(RepositorioDonaciones.class);
        when(repoDonaciones.obtenerPorId(donacionId)).thenReturn(Optional.of(donacion));

        RepositorioDeResultadosMatchmaking repoResultados = mock(RepositorioDeResultadosMatchmaking.class);
        when(repoResultados.findByDonacionId(donacionId))
                .thenReturn(Optional.of(mock(ResultadoMatchmaking.class)));

        GestorMatchmaking gestorMatchmaking = mock(GestorMatchmaking.class);
        when(gestorMatchmaking.obtenerPropuestaSeleccionadaParaDonacion(eq(donacionId), eq(1)))
                .thenReturn(mock(PropuestaAsignacion.class));

        ProductorLogistica productor = mock(ProductorLogistica.class);

        DonacionService service = new DonacionService(
                mock(GestorAsignaciones.class),
                mock(GestorFormulario.class),
                gestorMatchmaking,
                mock(RepositorioSubcategoriasDeBienes.class),
                repoDonaciones,
                mock(RepositorioDonantes.class),
                mock(RepositorioFormularios.class),
                mock(RepositorioEntidadesBeneficiarias.class),
                repoResultados,
                mock(RepositorioBienes.class),
                productor
        );

        service.asignarPropuesta(donacionId, 1);

        ArgumentCaptor<EntregaDTO> captor = ArgumentCaptor.forClass(EntregaDTO.class);
        verify(productor).publicarDonacionAsignada(captor.capture());

        // El JSON que sale por el broker tiene que satisfacer el contrato del receptor.
        String json = mapper.writeValueAsString(captor.getValue());
        EntregaMirror recibido = mapper.readValue(json, EntregaMirror.class);

        assertNotNull(recibido.donacionResumen, "logística necesita idsDonaciones y bienes");
        // Un item agregado por unidad: logística guarda UN ItemEntrega por donación (su clave
        // es el idDonacion), y el sumado tiene que representar TODOS los bienes del segmento
        // para que los bienes 2..N no se pierdan como "repetidos" (punto 31).
        assertEquals(List.of(donacionId), recibido.donacionResumen.idsDonaciones);
        assertEquals(1, recibido.donacionResumen.bienes.size());
        assertEquals(16, recibido.donacionResumen.bienes.get(0).cantidad);
        assertEquals("KILOGRAMOS", recibido.donacionResumen.bienes.get(0).unidadDeMedida);

        for (BienMirror bienRecibido : recibido.donacionResumen.bienes) {
            Set<ConstraintViolation<BienMirror>> violacionesBien = validator.validate(bienRecibido);
            assertTrue(violacionesBien.isEmpty(), () -> "violaciones en el bien: " + violacionesBien);
        }

        DireccionMirror direccionRecibida = recibido.entidadBeneficiaria;
        assertNotNull(direccionRecibida, "sin entidadBeneficiaria logística no resuelve el destino");
        Set<ConstraintViolation<DireccionMirror>> violaciones = validator.validate(direccionRecibida);
        assertTrue(violaciones.isEmpty(), () -> "violaciones de contrato: " + violaciones);
        assertEquals(entidad.getId(), direccionRecibida.idEntidad,
                "EntregaService.resolverEntidad hace findById(idEntidad): sin él el mensaje se descarta");
    }
}
