package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Servicio de entidades beneficiarias", description = "Endpoints para operaciones CRUD de Entidades beneficiarias y la administracion de sus necesidades")
@RequestMapping("/entidades")
public class EntidadBeneficiariaController {

    private final EntidadBeneficiariaService service;

    public EntidadBeneficiariaController(EntidadBeneficiariaService service) {
        this.service = service;
    }

    // --- ENDPOINTS DE ENTIDAD BENEFICIARIA ---

    @Operation(summary = "Ver entidades", description = "permite ver todas las entidades del repositorio de entidades")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "entidades obtenidas con exito"),
        @ApiResponse(responseCode = "400", description = "Error al tratar de obtener las entidades")
    })
    @GetMapping
    public ResponseEntity<List<EntidadBeneficiariaDTO>> obtenerTodas() {
        List<EntidadBeneficiariaDTO> dtos = service.obtenerTodas().stream()
                                                   .map(this::convertirADTO)
                                                   .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @Operation(summary = "Ver entidad por id", description = "permite buscar y obtener una entidad en base a su id")
    @GetMapping("/{id}")
    public ResponseEntity<EntidadBeneficiariaDTO> obtenerEntidad(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(convertirADTO(service.obtenerEntidadPorId(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Crear una Entidad", description = "permite registrar una nueva entidad")
    @PostMapping
    public ResponseEntity<?> registrarEntidad(@RequestBody EntidadBeneficiariaDTO dto) {
        try {
            EntidadBeneficiaria entidad = mapearADominio(dto);
            EntidadBeneficiaria registrada = service.registrarEntidad(entidad);
            return new ResponseEntity<>(convertirADTO(registrada), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "actualizar entidad", description = "permite actualizar una entidad buscandola por su id")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarEntidad(@PathVariable UUID id, @RequestBody EntidadBeneficiariaDTO dto) {
        try {
            EntidadBeneficiaria entidad = mapearADominio(dto);
            EntidadBeneficiaria actualizada = service.actualizarEntidad(id, entidad);
            return ResponseEntity.ok(convertirADTO(actualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "eliminar entidad")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEntidad(@PathVariable UUID id) {
        service.eliminarEntidad(id);
        return ResponseEntity.noContent().build();
    }

    // --- ENDPOINTS DE NECESIDADES ---

    @Operation(summary = "Ver necesidades")
    @GetMapping("/{id}/necesidades")
    public ResponseEntity<List<NecesidadDTO>> obtenerNecesidades(@PathVariable UUID id) {
        try {
            List<NecesidadDTO> dtos = service.obtenerNecesidades(id).stream()
                                             .map(this::convertirNecesidadADTO)
                                             .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "agregar necesidad")
    @PostMapping("/{id}/necesidades")
    public ResponseEntity<?> agregarNecesidad(@PathVariable UUID id, @RequestBody NecesidadDTO dto) {
        try {
            Necesidad necesidad = mapearNecesidadADominio(dto);
            Necesidad agregada = service.agregarNecesidad(id, necesidad);
            return new ResponseEntity<>(convertirNecesidadADTO(agregada), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "eliminar necesidad")
    @DeleteMapping("/{id}/necesidades/{idNecesidad}")
    public ResponseEntity<Void> eliminarNecesidad(@PathVariable UUID id, @PathVariable UUID idNecesidad) {
        try {
            service.eliminarNecesidad(id, idNecesidad);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- ENDPOINTS DE DONACIONES DE LA ENTIDAD ---

    @Operation(summary = "Ver donaciones")
    @GetMapping("/{id}/donaciones")
    public ResponseEntity<List<DonacionDTO>> obtenerDonaciones(@PathVariable UUID id) {
        try {
            List<DonacionDTO> dtos = service.obtenerDonaciones(id).stream()
                                            .map(this::convertirDonacionADTO)
                                            .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private EntidadBeneficiaria mapearADominio(EntidadBeneficiariaDTO dto) {
        DireccionDTO dirDTO = dto.getDireccion();
        Direccion direccion = null;
        if (dirDTO != null) {
            Pais pais = new Pais(dirDTO.getPais());
            Provincia provincia = new Provincia(dirDTO.getProvincia(), pais);
            Ciudad ciudad = new Ciudad(dirDTO.getCiudad(), provincia);
            direccion = new Direccion(
                dirDTO.getCalleUno(), dirDTO.getCalleDos(), dirDTO.getAltura(),
                dirDTO.getPiso(), dirDTO.getDepartamento(), ciudad
            );
        }
        return new EntidadBeneficiaria(direccion, new Telefono(dto.getTelefono()), null);
    }

    private EntidadBeneficiariaDTO convertirADTO(EntidadBeneficiaria entidad) {
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        if (entidad.getPersonaJuridica() != null) {
            dto.setRazonSocial(entidad.getPersonaJuridica().getRazonSocial());
        }
        dto.setTelefono(entidad.getNroTell() != null ? entidad.getNroTell().getValor() : null);
        return dto;
    }

    private Necesidad mapearNecesidadADominio(NecesidadDTO dto) {
        SubcategoriaBien subcategoria = new SubcategoriaBien(dto.getNombreSubcategoria(), new CategoriaBien(dto.getNombreCategoria()));
        return switch (dto.getTipoNecesidad().toUpperCase()) {
            case "RECURRENTE" -> new NecesidadRecurrente(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo(), dto.getPlazoEnDias()
            );
            case "EXTRAORDINARIA" -> new NecesidadExtraordinaria(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo()
            );
            default -> throw new IllegalArgumentException("Tipo de necesidad inválido: " + dto.getTipoNecesidad());
        };
    }

    private NecesidadDTO convertirNecesidadADTO(Necesidad necesidad) {
        NecesidadDTO dto = new NecesidadDTO();
        dto.setDescripcion(necesidad.getDescripcion());
        dto.setCantidadObjetivo(necesidad.getCantidadObjetivo());
        dto.setNombreSubcategoria(necesidad.getSubcategoria() != null ? necesidad.getSubcategoria().getNombre() : null);
        dto.setNombreCategoria(necesidad.getSubcategoria() != null && necesidad.getSubcategoria().getCategoria() != null
                               ? necesidad.getSubcategoria().getCategoria().getNombre() : null);
        dto.setTipoNecesidad(necesidad instanceof NecesidadRecurrente ? "RECURRENTE" : "EXTRAORDINARIA");
        if (necesidad instanceof NecesidadRecurrente recurrente) {
            dto.setPlazoEnDias(recurrente.getPlazoEnDias());
        }
        return dto;
    }

    private DonacionDTO convertirDonacionADTO(Donacion donacion) {
        DonacionDTO dto = new DonacionDTO();
        dto.setDonanteName(donacion.getDonante() != null ? donacion.getDonante().darNombre() : "Desconocido");
        if (donacion.getEntidad() != null && donacion.getEntidad().getPersonaJuridica() != null) {
            dto.setEntidadBeneficiaria(donacion.getEntidad().getPersonaJuridica().getRazonSocial());
        }
        dto.setDescripcion(donacion.getDescripcion());
        dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");
        dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
        dto.setCategoriaBienName(donacion.getSubcategoria() != null && donacion.getSubcategoria().getCategoria() != null
                                 ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");
        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
        return dto;
    }
}