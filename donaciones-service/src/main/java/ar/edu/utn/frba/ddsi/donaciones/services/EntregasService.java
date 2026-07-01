package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.clients.LogisticaClient;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.DireccionEntidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.DonacionResumenDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.EntregaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.InfoEntregasDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.InfoRutasDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.logistica.RutaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.Camion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.EstadoEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.DonacionResumen;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioNotificaciones.GestorNotificacionesEventos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class EntregasService {
    private static final int MAX_DONACIONES_POR_LOTE = 100;

    private final RepositorioDonaciones repositorio;
    private final RepositorioEntidadesBeneficiarias repositorioEntidades;
    private final GestorNotificacionesEventos gestorNotificaciones;
    private final LogisticaClient logisticaClient;
    private final RepositorioRutasActivas repositorioRutas;

    public EntregasService(RepositorioDonaciones repositorio,
                           RepositorioEntidadesBeneficiarias repositorioEntidades,
                           GestorNotificacionesEventos gestorNotificaciones,
                           LogisticaClient logistica,
                           RepositorioRutasActivas repoRutas) {
        this.repositorio = repositorio;
        this.repositorioEntidades = repositorioEntidades;
        this.gestorNotificaciones = gestorNotificaciones;
        this.logisticaClient = logistica;
        this.repositorioRutas = repoRutas;
    }

    public void conseguirInfoAsignacionRutas() {
        List<Donacion> donacionesEntregar = repositorio.findEntregarPendient();
        if (donacionesEntregar.isEmpty()) {
            return;
        }

        for (int inicio = 0; inicio < donacionesEntregar.size();
             inicio += MAX_DONACIONES_POR_LOTE) {
            int fin = Math.min(inicio + MAX_DONACIONES_POR_LOTE, donacionesEntregar.size());
            procesarLoteDonaciones(donacionesEntregar.subList(inicio, fin));
        }
    }

    private void procesarLoteDonaciones(List<Donacion> donaciones) {
        InfoEntregasDTO infoEntregas = convertirAInfoEntregas(donaciones);
        if (infoEntregas.getEntregas().isEmpty()) {
            return;
        }

        InfoRutasDTO infoRutas = logisticaClient.recibirInfoCreacionRutas(infoEntregas);
        registrarRutas(infoRutas);
    }

    private void registrarRutas(InfoRutasDTO infoRutas) {
        if (infoRutas == null || infoRutas.getRutas() == null) {
            return;
        }

        infoRutas.getRutas().stream()
                .filter(Objects::nonNull)
                .filter(ruta -> ruta.getIdRuta() != null)
                .map(this::convertirRutaEnProceso)
                .forEach(repositorioRutas::save);
        //save si lo encuentra x idRuta en el repo, lo remueve y lo vuelve a agregar
        //asi q si habia alguna novedad de la ruta se registra el ultimo estado



        //TODO revisar el repo por rutas con estado en_viaje para las notificaciones
        // notificar a cada entidad de la ruta

//        se notificará a cada entidad y personaDonante de la ruta sobre las entregas
//        formen parte de la ruta en viaje. La notificación deberá incluir un enlace al mapa
//        interactivo
//
        List<RutaEnProceso> rutasEnViaje = repositorioRutas.findAll().stream()
                .filter(ruta -> ruta.getEstadoEntrega() == EstadoEntrega.EN_VIAJE)
                .toList();


    }

    private InfoEntregasDTO convertirAInfoEntregas(List<Donacion> donaciones) {
        InfoEntregasDTO infoEntregas = new InfoEntregasDTO();
        infoEntregas.setEntregas(donaciones.stream()
                .filter(donacion -> donacion.getEntidad() != null)
                .map(this::convertirAEntregaDTO)
                .toList());
        return infoEntregas;
    }

    private EntregaDTO convertirAEntregaDTO(Donacion donacion) {
        EntregaDTO entrega = new EntregaDTO();
        entrega.setIdsDonaciones(List.of(donacion.getId()));
        entrega.setDonacionResumen(donacion.getBienes().stream()
                .map(this::convertirADonacionResumenDTO)
                .toList());
        entrega.setEntidadBeneficiaria(convertirADireccionEntidadDTO(donacion.getEntidad()));
        return entrega;
    }

    private DonacionResumenDTO convertirADonacionResumenDTO(Bien bien) {
        DonacionResumenDTO resumen = new DonacionResumenDTO();
        resumen.setPeso(bien.getPeso());
        resumen.setUnidadDeMedida(bien.getUnidadUtilizada() != null ? bien.getUnidadUtilizada().name() : null);
        return resumen;
    }

    private DireccionEntidadDTO convertirADireccionEntidadDTO(EntidadBeneficiaria entidad) {
        DireccionEntidadDTO direccionEntidad = new DireccionEntidadDTO();
        direccionEntidad.setNombreEntidad(entidad.getRazonSocial());
        direccionEntidad.setDireccion(convertirADireccionDTO(entidad.getDireccion()));
        return direccionEntidad;
    }

    private DireccionDTO convertirADireccionDTO(Direccion direccion) {
        DireccionDTO direccionDTO = new DireccionDTO();
        if (direccion == null) {
            return direccionDTO;
        }

        direccionDTO.setCalleUno(direccion.getCalleUno());
        direccionDTO.setCalleDos(direccion.getCalleDos());
        direccionDTO.setAltura(direccion.getAltura());
        direccionDTO.setPiso(direccion.getPiso());
        direccionDTO.setDepartamento(direccion.getDepartamento());

        if (direccion.getCiudad() != null) {
            direccionDTO.setCiudad(direccion.getCiudad().getNombre());
            if (direccion.getCiudad().getProvincia() != null) {
                direccionDTO.setProvincia(direccion.getCiudad().getProvincia().getNombre());
                if (direccion.getCiudad().getProvincia().getPais() != null) {
                    direccionDTO.setPais(direccion.getCiudad().getProvincia().getPais().getNombre());
                }
            }
        }

        return direccionDTO;
    }

    private RutaEnProceso convertirRutaEnProceso(RutaDTO rutaDTO) {
        RutaEnProceso ruta = new RutaEnProceso();
        ruta.setIdRuta(rutaDTO.getIdRuta());
        ruta.setIdEntrega(repositorioRutas.findByIdRuta(rutaDTO.getIdRuta())
                .map(RutaEnProceso::getIdEntrega)
                .orElseGet(UUID::randomUUID));
        ruta.setPaquete(convertirAEntrega(rutaDTO.getPaquete()));
        ruta.setCamionEntrega(convertirACamion(rutaDTO.getCamionEntrega()));
        ruta.setUrlSeguimiento(rutaDTO.getUrlSeguimiento());
        ruta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        return ruta;
    }

    private ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.Entrega convertirAEntrega(EntregaDTO entregaDTO) {
        if (entregaDTO == null) {
            return null;
        }

        ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.Entrega entrega =
                new ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.Entrega();
        entrega.setIdsDonaciones(entregaDTO.getIdsDonaciones());
        entrega.setDonacionResumen(entregaDTO.getDonacionResumen() != null
                ? entregaDTO.getDonacionResumen().stream()
                        .filter(Objects::nonNull)
                        .map(this::convertirADonacionResumen)
                        .toList()
                : List.of());
        entrega.setEntidadBeneficiaria(convertirADireccionEntidad(entregaDTO.getEntidadBeneficiaria()));
        return entrega;
    }

    private DonacionResumen convertirADonacionResumen(DonacionResumenDTO resumenDTO) {
        DonacionResumen resumen = new DonacionResumen();
        resumen.setPeso(resumenDTO.getPeso());
        resumen.setUnidadDeMedida(resumenDTO.getUnidadDeMedida());
        return resumen;
    }

    private ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.DireccionEntidad convertirADireccionEntidad(
            DireccionEntidadDTO direccionEntidadDTO
    ) {
        if (direccionEntidadDTO == null) {
            return null;
        }

        ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.DireccionEntidad direccionEntidad =
                new ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.DireccionEntidad();
        direccionEntidad.setNombreEntidad(direccionEntidadDTO.getNombreEntidad());
        direccionEntidad.setDireccion(convertirADireccion(direccionEntidadDTO.getDireccion()));
        return direccionEntidad;
    }

    private Direccion convertirADireccion(DireccionDTO direccionDTO) {
        if (direccionDTO == null) {
            return null;
        }

        Pais pais = new Pais(direccionDTO.getPais());
        Provincia provincia = new Provincia(direccionDTO.getProvincia(), pais);
        Ciudad ciudad = new Ciudad(direccionDTO.getCiudad(), provincia);
        return new Direccion(
                direccionDTO.getCalleUno(),
                direccionDTO.getCalleDos(),
                direccionDTO.getAltura(),
                direccionDTO.getPiso() != null ? direccionDTO.getPiso() : 0,
                direccionDTO.getDepartamento(),
                ciudad
        );
    }

    private Camion convertirACamion(ar.edu.utn.frba.ddsi.donaciones.dto.logistica.CamionDisponibleDTO camionDTO) {
        if (camionDTO == null) {
            return null;
        }

        Camion camion = new Camion();
        camion.setNombreChofer(camionDTO.getNombreChofer());
        camion.setPatente(camionDTO.getPatente());
        camion.setInicioRuta(camionDTO.getInicioRuta());
        return camion;
    }
}
