package ar.edu.utn.frba.ddsi.logisticas.services;

import ar.edu.utn.frba.ddsi.logisticas.dto.*;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Chofer.Chofer;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.EstadoEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Parada.Parada;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.EstadoRuta;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Ruta.Ruta;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCamiones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioChoferes;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioRutas;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LogisticaService {
    private final RepositorioCamiones repositorioCamiones;
    private final RepositorioItemEntrega repositorioItemEntrega;
    private final RepositorioChoferes repositorioChoferes;
    private final RepositorioRutas repositorioRutas;

    public LogisticaService(RepositorioCamiones repositorioCamiones,
                            RepositorioItemEntrega repositorioItemEntrega,
                            RepositorioChoferes repositorioChoferes,
                            RepositorioRutas repositorioRutas) {
        this.repositorioCamiones = repositorioCamiones;
        this.repositorioItemEntrega = repositorioItemEntrega;
        this.repositorioChoferes = repositorioChoferes;
        this.repositorioRutas = repositorioRutas;
    }

    public void procesarPeticion(PeticionEntregaDTO request){
        List<EntregaDTO> entregas = request.getEntregas();
        for (EntregaDTO entregaActual : entregas) {
            List<BienDTO> bienes = entregaActual.getDonacionResumen();
            for (int j = 0; j < bienes.size(); j++) {
                BienDTO bien = bienes.get(j);
                Direccion direccionEntidad = this.convertirDireccionDTO(entregaActual.getEntidadBeneficiaria());
                repositorioItemEntrega.save(new ItemEntrega(entregaActual.getIdsDonaciones().get(j), bien.getCantidad(), UnidadDeMedida.valueOf(bien.getUnidadDeMedida()), new Entidad(entregaActual.getEntidadBeneficiaria().getIdEntidad(), direccionEntidad)));
            }
        }
    }

    public Camion convertirDTO(CamionDTO dto){
        return new Camion(null, dto.getPatente(), dto.getCapacidadVolumen(), dto.getAltura(), dto.getCapacidadCarga(), dto.getDisponible());
    }

    public CamionDTO convertirACamionDTO(Camion camion){
        CamionDTO dto = new CamionDTO();
        dto.setNombreChofer(camion.getChofer());
        dto.setPatente(camion.getPatente());
        dto.setCapacidadVolumen(camion.getCapacidadVolumen());
        dto.setAltura(camion.getAltura());
        dto.setCapacidadCarga(camion.getCapacidadCarga());
        dto.setDisponible(camion.getDisponible());
        return dto;
    }

    public Direccion convertirDireccionDTO(DireccionDTO dto){
        return new Direccion(dto.getCalleUno(), dto.getCalleDos(), dto.getAltura(), dto.getPiso(), dto.getDepartamento(), dto.getCiudad(), dto.getProvincia(), dto.getPais());
    }

    public void guardarCamiones(List<Camion> camiones){
        this.repositorioCamiones.addAll(camiones);
    }

    public Chofer convertirChoferDTO(ChoferDTO dto){
        return new Chofer(dto.getIdChofer(), this.convertirDTO(dto.getCamionAsignado()));
    }

    public ChoferDTO convertirAChoferDTO(Chofer chofer){
        ChoferDTO dto = new ChoferDTO();
        dto.setIdChofer(chofer.getIdChofer());
        dto.setDisponible(chofer.isDisponible());
        dto.setCamionAsignado(this.convertirACamionDTO(chofer.getCamionAsignado()));
        return dto;
    }

    public void guardarChoferes(List<Chofer> choferes){
        this.repositorioChoferes.addAll(choferes);
    }

    public void iniciarRuta(UUID idChofer) {
        Chofer chofer = repositorioChoferes.findById(idChofer);
        Ruta rutaActual = this.obtenerRutaActual(chofer);

        repositorioRutas.actualizarEstado(rutaActual, EstadoRuta.EN_CURSO);

        rutaActual.getParadas().forEach(parada ->
                parada.getItems().forEach(item -> {
                    item.setEstado(EstadoEntrega.EN_TRASLADO);
                    repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.EN_TRASLADO);
                })
        );
    }

    public Ruta obtenerRutaActual(Chofer chofer) {
        return repositorioRutas.findAll().stream()
                .filter(ruta -> ruta.getCamionAsignado().equals(chofer.getCamionAsignado()))
                .findFirst()
                .orElse(null);
    }

    public void terminarRuta(UUID idChofer) {
        Chofer chofer = repositorioChoferes.findById(idChofer);
        Ruta rutaActual = obtenerRutaActual(chofer);
        if (rutaActual != null) {
            repositorioRutas.actualizarEstado(rutaActual, EstadoRuta.FINALIZADA);
            for(Parada parada : rutaActual.getParadas()){
                for(ItemEntrega item : parada.getItems()){
                    if(item.getEstado() != EstadoEntrega.ENTREGADA){
                        repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.PENDIENTE);
                    } else {
                        repositorioItemEntrega.findAll().remove(item);
                    }
                }
            }
        }
        repositorioCamiones.actualizarcarga(chofer.getCamionAsignado());
        repositorioChoferes.actualizarCamion(chofer);
    }

    public void entregarPaquete(UUID idDonacion){
        ItemEntrega item = repositorioItemEntrega.findById(idDonacion);
        repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.ENTREGADA);
    }

    public void paqueteNoRecibido(UUID idDonacion){
        ItemEntrega item = repositorioItemEntrega.findById(idDonacion);
        repositorioItemEntrega.actualizarEstado(item, EstadoEntrega.NO_RECIBIDA);
    }
}