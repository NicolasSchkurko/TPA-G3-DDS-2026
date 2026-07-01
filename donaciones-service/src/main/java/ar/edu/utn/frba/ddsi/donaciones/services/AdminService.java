package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Estado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.EstadoEntrega;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Entregas.RutaEnProceso;

import java.util.List;
import java.util.UUID;

public class AdminService {
//    public void registrarRegresoADeposito(UUID idEntrega) {
//        RutaEnProceso ruta = repositorioRutasActivas.findByIdEntrega(idEntrega)
//                .orElseThrow(() -> new IllegalArgumentException("No se encontro la entrega con ID: " + idEntrega));
//
//        List<UUID> idsDonaciones = ruta.getPaquete() != null ? ruta.getPaquete().getIdsDonaciones() : List.of();
//        idsDonaciones.stream()
//                .map(repositorioDonaciones::findById)
//                .flatMap(java.util.Optional::stream)
//                .forEach(donacion -> donacion.actualizarEstado(
//                        Estado.EN_DEPOSITO,
//                        "La donacion regreso al deposito luego de una entrega no recibida"
//                ));
//
//        ruta.setEstadoEntrega(EstadoEntrega.PENDIENTE);
//        repositorioRutasActivas.save(ruta);
//    }
}
