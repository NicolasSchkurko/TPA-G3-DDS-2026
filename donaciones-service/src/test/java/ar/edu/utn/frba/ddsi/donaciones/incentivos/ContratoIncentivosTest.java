package ar.edu.utn.frba.ddsi.donaciones.incentivos;

import ar.edu.utn.frba.ddsi.donaciones.clients.IncentivosClient;
import ar.edu.utn.frba.ddsi.donaciones.clients.NotificacionesClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IDDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.IncentivosDonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.personaDonante.PersonaDonanteDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorAsignaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioDonantes;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioNecesidades;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.repos.RepositorioPersonas;
import ar.edu.utn.frba.ddsi.donaciones.services.DonanteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrato del payload que este servicio le manda a incentivos en
 * PATCH /api/perfiles/donacion/{idUsuario}.
 *
 * <p>El contrato real vive en incentivos-service
 * ({@code dto.Persona.ImpactoDonacionDTO}); acá se mira con una copia mínima del
 * lado receptor (ver punto 27: DTOs duplicados a mano). Si este test falla, la
 * asignación entera muere con un 400 del otro lado.
 */
public class ContratoIncentivosTest {

    private final ObjectMapper mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // Copia mínima del ImpactoDonacionDTO de incentivos-service (el lado que recibe).
    public static class ImpactoDonacionMirror {
        @NotNull(message = "La donación requiere su id de origen")
        private UUID idDonacion;

        @NotNull(message = "La donación requiere una fecha de entrega")
        private LocalDateTime fechaEntrega;

        @NotNull(message = "La donación requiere la cantidad de bienes")
        @PositiveOrZero(message = "La cantidad de bienes no puede ser negativa")
        private Integer cantidadBienes;

        private String subCategoria;

        @NotBlank(message = "La donación requiere una categoría")
        private String categoria;

        @NotBlank(message = "La donación requiere una entidad beneficiaria")
        private String entidadBeneficiaria;

        @NotBlank(message = "La donación requiere un estado")
        private String estado;

        public UUID getIdDonacion() { return idDonacion; }
        public void setIdDonacion(UUID idDonacion) { this.idDonacion = idDonacion; }
        public LocalDateTime getFechaEntrega() { return fechaEntrega; }
        public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
        public Integer getCantidadBienes() { return cantidadBienes; }
        public void setCantidadBienes(Integer cantidadBienes) { this.cantidadBienes = cantidadBienes; }
        public String getSubCategoria() { return subCategoria; }
        public void setSubCategoria(String subCategoria) { this.subCategoria = subCategoria; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public String getEntidadBeneficiaria() { return entidadBeneficiaria; }
        public void setEntidadBeneficiaria(String entidadBeneficiaria) { this.entidadBeneficiaria = entidadBeneficiaria; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
    }

    // Copia mínima del PerfilDonanteDTO de incentivos-service (POST /api/perfiles).
    public static class PerfilDonanteMirror {
        @NotNull(message = "El donante requiere un id de usuario")
        private UUID idUsuario;
        @NotBlank(message = "El donante requiere un nombre de usuario")
        private String nombreUsuario;
        private String role;

        public UUID getIdUsuario() { return idUsuario; }
        public void setIdUsuario(UUID idUsuario) { this.idUsuario = idUsuario; }
        public String getNombreUsuario() { return nombreUsuario; }
        public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    // Copia mínima del AltaPerfilesLoteDTO de incentivos-service (POST /api/perfiles/lote).
    public static class AltaPerfilesLoteMirror {
        @jakarta.validation.Valid
        @jakarta.validation.constraints.NotEmpty
        private List<PerfilDonanteMirror> perfiles;

        public List<PerfilDonanteMirror> getPerfiles() { return perfiles; }
        public void setPerfiles(List<PerfilDonanteMirror> perfiles) { this.perfiles = perfiles; }
    }

    @Test
    @DisplayName("El payload del alta en lote cumple el contrato del endpoint /api/perfiles/lote")
    void altaEnLote_cumpleContratoDeIncentivos() throws Exception {
        ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.AltaPerfilesLoteDTO lote =
                new ar.edu.utn.frba.ddsi.donaciones.dto.incentivos.AltaPerfilesLoteDTO(List.of(
                        new IDDTO(UUID.randomUUID(), "Sofia Garcia", "DONANTE"),
                        new IDDTO(UUID.randomUUID(), "Ana Navarro", "DONANTE")));

        String json = mapper.writeValueAsString(lote);
        AltaPerfilesLoteMirror recibido = mapper.readValue(json, AltaPerfilesLoteMirror.class);

        assertEquals(2, recibido.getPerfiles().size());
        assertEquals("Sofia Garcia", recibido.getPerfiles().get(0).getNombreUsuario());
        assertEquals("DONANTE", recibido.getPerfiles().get(1).getRole());

        Set<ConstraintViolation<AltaPerfilesLoteMirror>> violations = validator.validate(recibido);
        assertTrue(violations.isEmpty(), () -> "violaciones de contrato: " + violations);
    }

    private Donacion donacionAsignable(LocalDate fechaEntrega) {
        Donacion donacion = new Donacion();
        donacion.setId(UUID.randomUUID());
        donacion.setFechaEntrega(fechaEntrega);
        donacion.setBienes(new ArrayList<>()); // sumaCantidadBienes -> 0

        SubcategoriaBien subcategoria = mock(SubcategoriaBien.class);
        CategoriaBien categoria = mock(CategoriaBien.class);
        when(subcategoria.getNombre()).thenReturn("Almacenes");
        when(subcategoria.getCategoria()).thenReturn(categoria);
        when(categoria.getNombre()).thenReturn("Alimentos");
        donacion.setSubcategoria(subcategoria);

        EntidadBeneficiaria entidad = mock(EntidadBeneficiaria.class);
        Juridica juridica = mock(Juridica.class);
        when(entidad.getPersonaJuridica()).thenReturn(juridica);
        when(juridica.getRazonSocial()).thenReturn("Fundación X");
        donacion.setEntidad(entidad);

        Donante donante = mock(Donante.class);
        when(donante.getId()).thenReturn(UUID.randomUUID());
        donacion.setDonante(donante);

        return donacion;
    }

    @Test
    @DisplayName("El alta de perfil incluye idUsuario, nombreUsuario y role DONANTE")
    void altaDePerfil_cumpleContratoDeIncentivos() throws Exception {
        IncentivosClient clienteAlta = mock(IncentivosClient.class);
        DonanteService donanteService = new DonanteService(
                mock(RepositorioPersonas.class),
                mock(RepositorioCiudades.class),
                mock(FabricaEstrategiasNotificacion.class),
                clienteAlta,
                mock(RepositorioDonantes.class),
                mock(java.util.concurrent.ExecutorService.class)
        );

        PersonaDonanteDTO alta = new PersonaDonanteDTO();
        alta.setTipoPersona("HUMANA");
        alta.setNombre("Sofia");
        alta.setApellido("Garcia");
        alta.setEdad(30);
        alta.setNumeroDeDocumento(30456789);
        PersonaDonanteDTO creada = donanteService.crearPersona(alta);

        ArgumentCaptor<IDDTO> captor = ArgumentCaptor.forClass(IDDTO.class);
        verify(clienteAlta).peticionCrearPerfil(captor.capture());

        // El JSON que saldría por HTTP tiene que satisfacer el contrato del receptor.
        String json = mapper.writeValueAsString(captor.getValue());
        PerfilDonanteMirror recibido = mapper.readValue(json, PerfilDonanteMirror.class);

        assertEquals(creada.getId(), recibido.getIdUsuario(),
                "el perfil se inicia con el id del donante: es la clave del lado de incentivos");
        assertEquals("Sofia Garcia", recibido.getNombreUsuario(),
                "nombre y apellido para una Humana");
        assertEquals("DONANTE", recibido.getRole());

        Set<ConstraintViolation<PerfilDonanteMirror>> violations = validator.validate(recibido);
        assertTrue(violations.isEmpty(), () -> "violaciones de contrato: " + violations);
    }

    @Test
    @DisplayName("El payload de la asignación incluye idDonacion y fechaEntrega en formato fecha-hora")
    void payloadDeAsignacion_cumpleContratoDeIncentivos() throws Exception {
        Donacion donacion = donacionAsignable(LocalDate.of(2026, 10, 7));

        RepositorioDonaciones repositorioDonaciones = mock(RepositorioDonaciones.class);
        when(repositorioDonaciones.obtenerPorId(donacion.getId())).thenReturn(Optional.of(donacion));

        IncentivosClient incentivosClient = mock(IncentivosClient.class);
        GestorAsignaciones gestor = new GestorAsignaciones(
                mock(NotificacionesClient.class),
                repositorioDonaciones,
                mock(RepositorioNecesidades.class),
                incentivosClient,
                mock(FabricaEstrategiasNotificacion.class)
        );

        gestor.cambiarEstado(donacion.getId(), "ASIGNADO", "test de contrato");

        ArgumentCaptor<IncentivosDonacionDTO> captor = ArgumentCaptor.forClass(IncentivosDonacionDTO.class);
        verify(incentivosClient, times(1)).notificarDonacionAsignada(eq(donacion.getDonante().getId()), captor.capture());

        // El JSON que saldría por HTTP tiene que satisfacer el contrato del receptor.
        String json = mapper.writeValueAsString(captor.getValue());
        ImpactoDonacionMirror recibido = mapper.readValue(json, ImpactoDonacionMirror.class);

        assertEquals(donacion.getId(), recibido.getIdDonacion(),
                "incentivos exige idDonacion: es su clave de idempotencia");
        assertEquals(LocalDateTime.of(2026, 10, 7, 0, 0), recibido.getFechaEntrega(),
                "incentivos espera LocalDateTime, no LocalDate");

        Set<ConstraintViolation<ImpactoDonacionMirror>> violations = validator.validate(recibido);
        assertTrue(violations.isEmpty(), () -> "violaciones de contrato: " + violations);
    }

    @Test
    @DisplayName("La fecha de realización del formulario queda como fechaEntrega de cada donación segmentada")
    void fechaRealizacionDelFormulario_quedaEnCadaDonacionSegmentada() {
        Donante donante = mock(Donante.class);

        SubcategoriaBien subcategoria = mock(SubcategoriaBien.class);
        when(subcategoria.getNombre()).thenReturn("Ropa");
        Bien bien = mock(Bien.class);
        when(bien.getSubcategoria()).thenReturn(subcategoria);

        LocalDate fechaRealizacion = LocalDate.of(2026, 10, 1);
        Formulario formulario = new Formulario(donante, List.of(bien), fechaRealizacion);

        List<Donacion> donaciones = new DonacionFacade(new SegmentadorDonaciones()).crearDonaciones(formulario);

        assertFalse(donaciones.isEmpty());
        for (Donacion donacion : donaciones) {
            assertEquals(fechaRealizacion, donacion.getFechaEntrega(),
                    "sin este valor, el reporte a incentivos sale con fecha nula (punto 10)");
        }
    }
}
