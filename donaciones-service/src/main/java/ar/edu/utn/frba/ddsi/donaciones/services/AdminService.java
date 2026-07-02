package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.EstadoEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioRutasActivas;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService {
    private final RepositorioRutasActivas repositorioRutasActivas;
    private final RepositorioDonaciones repositorioDonaciones;

    public AdminService(RepositorioRutasActivas repositorioRutasActivas,
                        RepositorioDonaciones repositorioDonaciones) {
        this.repositorioRutasActivas = repositorioRutasActivas;
        this.repositorioDonaciones = repositorioDonaciones;
    }

    public List<RutaEnProceso> obtenerEntregasNoRecibidas() {
        return repositorioRutasActivas.findAll().stream()
                .filter(ruta -> ruta.getEstadoEntrega() == EstadoEntrega.NO_RECIBIDA)
                .toList();
    }

    public void revisarEntregaNoRecibida(UUID idEntrega, String nuevoEstado) {
        RutaEnProceso ruta = repositorioRutasActivas.findByIdEntrega(idEntrega)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entrega con ID: " + idEntrega));

        if (ruta.getEstadoEntrega() != EstadoEntrega.NO_RECIBIDA) {
            throw new IllegalArgumentException("Solo se pueden revisar entregas NO_RECIBIDA");
        }

        EstadoEntrega estadoEntrega = convertirEstadoEntrega(nuevoEstado);
        if (estadoEntrega == EstadoEntrega.PENDIENTE) {
            registrarRegresoADeposito(ruta);
            return;
        }

        //no se como revisara las de replanificar

        ruta.setEstadoEntrega(estadoEntrega);
        repositorioRutasActivas.save(ruta);
    }

    private void registrarRegresoADeposito(RutaEnProceso ruta) {
        List<UUID> idsDonaciones = ruta.getPaquete() != null ? ruta.getPaquete().getIdsDonaciones() : List.of();
        idsDonaciones.stream()
                .map(repositorioDonaciones::findById)
                .flatMap(java.util.Optional::stream)
                .forEach(donacion -> donacion.actualizarEstado(
                        Estado.EN_DEPOSITO,
                        "La donacion regreso al deposito luego de una entrega no recibida"
                ));

        ruta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
        repositorioRutasActivas.save(ruta);
    }

    private EstadoEntrega convertirEstadoEntrega(String estadoEntrega) {
        if (estadoEntrega == null) {
            throw new IllegalArgumentException("El estado de entrega es obligatorio");
        }

        return switch (estadoEntrega.toUpperCase()) {
            case "PENDIENTE" -> EstadoEntrega.PENDIENTE; //podria o no add + casos de revision
//            case "EN_VIAJE", "EN VIAJE" -> EstadoEntrega.EN_VIAJE;
//            case "NO_RECIBIDA", "NO RECIBIDA" -> EstadoEntrega.NO_RECIBIDA;
            default -> throw new IllegalArgumentException("Estado de entrega invalido para revision: " + estadoEntrega);
        };
    }
}
