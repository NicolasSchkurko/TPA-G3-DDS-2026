package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.RecepcionEntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Administrador;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.EstadoEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.GestorNotificacionesEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioRutasActivas;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.Necesidades.Necesidad;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final RepositorioEntidadesBeneficiarias repositorio;
    private final RepositorioRutasActivas repositorioRutasActivas;
    private final RepositorioDonaciones repositorioDonaciones;
    private final GestorNotificacionesEventos gestorNotificaciones;
    private final Administrador admin;

    public EntidadBeneficiariaService(RepositorioEntidadesBeneficiarias repositorio,
                                      RepositorioRutasActivas repositorioRutasActivas,
                                      RepositorioDonaciones repositorioDonaciones,
                                      GestorNotificacionesEventos gestorNotificaciones,
                                      Administrador admin) {
        this.repositorio = repositorio;
        this.repositorioRutasActivas = repositorioRutasActivas;
        this.repositorioDonaciones = repositorioDonaciones;
        this.gestorNotificaciones = gestorNotificaciones;
        this.admin = admin;
    }

    // --- OPERACIONES CRUD ENTIDADES ---

    public List<EntidadBeneficiariaDTO> obtenerTodas() {
        return repositorio.findAll().stream()
                          .map(this::convertirADTO)
                          .collect(Collectors.toList());
    }

    public EntidadBeneficiariaDTO obtenerEntidadPorId(UUID id) {
        return repositorio.findById(id)
                          .map(this::convertirADTO)
                          .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + id));
    }

    public EntidadBeneficiariaDTO registrarEntidad(EntidadBeneficiariaDTO dto) {
        DireccionDTO dirDTO = dto.getDireccion();

        Pais pais = new Pais(dirDTO.getPais());
        Provincia provincia = new Provincia(dirDTO.getProvincia(), pais);
        Ciudad ciudad = new Ciudad(dirDTO.getCiudad(), provincia);

        Direccion direccion = new Direccion(
            dirDTO.getCalleUno(), dirDTO.getCalleDos(), dirDTO.getAltura(),
            dirDTO.getPiso(), dirDTO.getDepartamento(), ciudad
        );

        EntidadBeneficiaria entidad = new EntidadBeneficiaria(
            dto.getRazonSocial(), direccion, new Telefono(dto.getTelefono()), null
        );

        EntidadBeneficiaria guardada = repositorio.save(entidad);
        return convertirADTO(guardada);
    }

    public EntidadBeneficiariaDTO actualizarEntidad(UUID id, EntidadBeneficiariaDTO dto) {
        EntidadBeneficiaria existente = repositorio.findById(id)
                                                   .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad con ID: " + id));

        existente.setRazonSocial(dto.getRazonSocial());
        existente.setNroTell(new Telefono(dto.getTelefono()));
        // Actualizar dirección si es necesario...

        return convertirADTO(repositorio.save(existente));
    }

    public void eliminarEntidad(UUID id) {
        repositorio.deleteById(id);
    }

    // --- OPERACIONES CRUD NECESIDADES ---

    public List<NecesidadDTO> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        return entidad.getNecesidades().stream()
                      .map(this::convertirNecesidadADTO)
                      .collect(Collectors.toList());
    }

    public NecesidadDTO agregarNecesidad(UUID idEntidad, NecesidadDTO dto) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        SubcategoriaBien subcategoria = new SubcategoriaBien(dto.getNombreSubcategoria(), new CategoriaBien(dto.getNombreCategoria()));

        Necesidad necesidad = switch (dto.getTipoNecesidad().toUpperCase()) {
            case "RECURRENTE" -> new NecesidadRecurrente(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo(), dto.getPlazoEnDias()
            );
            case "EXTRAORDINARIA" -> new NecesidadExtraordinaria(
                subcategoria, dto.getDescripcion(), dto.getCantidadObjetivo()
            );
            default -> throw new IllegalArgumentException("Tipo de necesidad inválido: " + dto.getTipoNecesidad());
        };

        entidad.agregarNecesidad(necesidad);
        repositorio.save(entidad); // Guardar cambios en el repo

        return convertirNecesidadADTO(necesidad);
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                                     .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad"));

        entidad.eliminarNecesidad(necesidad);
        repositorio.save(entidad);
    }

    // --- OTROS MÉTODOS ---

    public List<DonacionDTO> obtenerDonaciones(UUID idEntidad) {
        EntidadBeneficiaria entidad = repositorio.findById(idEntidad)
                                                 .orElseThrow(() -> new IllegalArgumentException("No se encontró la entidad"));

        return entidad.verDonaciones().stream()
                      .map(this::convertirDonacionADTO)
                      .collect(Collectors.toList());
    }

    // --- MAPPERS INTERNOS ---

    private EntidadBeneficiariaDTO convertirADTO(EntidadBeneficiaria entidad) {
        EntidadBeneficiariaDTO dto = new EntidadBeneficiariaDTO();
        dto.setRazonSocial(entidad.getRazonSocial());
        dto.setTelefono(entidad.getNroTell() != null ? entidad.getNroTell().getNumeroDeTelefono() : null);
        return dto;
    }

    private NecesidadDTO convertirNecesidadADTO(Necesidad necesidad) {
        NecesidadDTO dto = new NecesidadDTO();
        // dto.setId(necesidad.getId()); // Añadir si agregas ID al DTO
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
        dto.setEntidadBeneficiaria(donacion.getEntidad() != null ? donacion.getEntidad().getRazonSocial() : null);
        dto.setDescripcion(donacion.getDescripcion());
        dto.setEstado(donacion.getEstado() != null ? donacion.getEstado().name() : "N/A");

        dto.setSubcategoriaName(donacion.getSubcategoria() != null ? donacion.getSubcategoria().getNombre() : "N/A");
        dto.setCategoriaBienName(donacion.getSubcategoria() != null && donacion.getSubcategoria().getCategoria() != null
                                 ? donacion.getSubcategoria().getCategoria().getNombre() : "N/A");

        dto.setFechaEntrega(donacion.getFechaEntrega());
        dto.setCantidadTotalBienes(donacion.sumaCantidadBienes());
        return dto;
    }

    public void ingresarEstadoEntrega(UUID idEntrega, RecepcionEntregaDTO dto){
        if (dto == null) {
            throw new IllegalArgumentException("La recepcion de entrega es obligatoria");
        }

        EstadoEntrega estadoEntrega = convertirEstadoEntrega(dto.getEstadoEntrega());
        RutaEnProceso ruta = repositorioRutasActivas.findByIdEntrega(idEntrega)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entrega con ID: " + idEntrega));

        ruta.setEstadoEntrega(estadoEntrega);
        ruta.setUrlImagenesEntrega(dto.getUrlImagenesEntrega());
        ruta.setFechaEntrega(dto.getFechaEntrega());
        ruta.setHoraEntrega(dto.getHoraEntrega());

        List<UUID> idsDonaciones = ruta.getPaquete() != null ? ruta.getPaquete().getIdsDonaciones() : List.of();
        EntidadBeneficiaria ent = repositorio.findById(ruta.getPaquete().getEntidadBeneficiaria().getIdEntidad()).get();

        if (estadoEntrega == EstadoEntrega.ENTREGADA) {
            idsDonaciones.stream()
                    .map(repositorioDonaciones::findById)
                    .flatMap(java.util.Optional::stream)
                    .forEach(donacion -> donacion.actualizarEstado(
                            Estado.ENTREGADO,
                            "Entrega confirmada por la entidad beneficiaria"
                    ));

            //TODO cuando la donacion esta entregada, la entidad puede cubrir algunas de sus
            //necesidades. Cuando cubra sus necesidades, se elimina de su lista
            //y se elimina la donacion del repo de donaciones


            gestorNotificaciones.notificarComprobanteEntregaAEntidadBeneficiaria(
                    ent.getCorreosRepresentantes(),
                    ruta
            );

            for(UUID idDonacion : idsDonaciones){
                repositorioDonaciones.findById(idDonacion)
                        .map(donacion -> donacion.getDonante().getMediosDeContacto())
                        .filter(Objects::nonNull)
                        .ifPresent(contacto ->
                                gestorNotificaciones.notificarComprobanteEntregaAPersonaDonante(
                                        contacto,
                                        ruta
                                ));
            }
            repositorioRutasActivas.deleteByIdEntrega(idEntrega);
            return;
        }

        if (estadoEntrega == EstadoEntrega.NO_RECIBIDA) {
            gestorNotificaciones.notificarEntregaNoRecibidaAEntidadBeneficiaria(
                    ent.getCorreosRepresentantes(),
                    ruta
            );

            gestorNotificaciones.notificarEntregaNoRecibidaAdmin(
                    admin.getMedioDeContacto(),
                    ruta
            );

            for(UUID idDonacion : idsDonaciones){
                repositorioDonaciones.findById(idDonacion)
                        .map(donacion -> donacion.getDonante().getMediosDeContacto())
                        .filter(Objects::nonNull)
                        .ifPresent(contacto ->
                                gestorNotificaciones.notificarEntregaNoRecibidaAPersonaDonante(
                                        contacto,
                                        ruta
                                ));
            }
            repositorioRutasActivas.deleteByIdEntrega(idEntrega);
            repositorioRutasActivas.save(ruta);
        }
    }

    private EstadoEntrega convertirEstadoEntrega(String estadoEntrega) {
        if (estadoEntrega == null) {
            throw new IllegalArgumentException("El estado de entrega es obligatorio");
        }

        EstadoEntrega estado;
        estado = switch (estadoEntrega.toUpperCase()) {
            case "ENTREGADA", "ENTREGADO" -> EstadoEntrega.ENTREGADA;
            case "NO_RECIBIDA", "NO RECIBIDA" -> EstadoEntrega.NO_RECIBIDA;
            default -> throw new IllegalArgumentException("Estado de entrega invalido: " + estadoEntrega);
        };

        if (estado != EstadoEntrega.ENTREGADA && estado != EstadoEntrega.NO_RECIBIDA) {
            throw new IllegalArgumentException("La entidad solo puede informar ENTREGADA o NO_RECIBIDA");
        }

        return estado;
    }
}
