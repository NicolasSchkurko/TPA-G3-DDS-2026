package ar.edu.utn.frba.ddsi.logisticas.broker;

import ar.edu.utn.frba.ddsi.logisticas.broker.adapters.ServicioLogisticaAdapter;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.PeticionEntregaDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class BrokerLogistica {

    private final Map<String, ServicioLogisticaAdapter> proveedores;

    public BrokerLogistica(List<ServicioLogisticaAdapter> listaProveedores) {
        this.proveedores = listaProveedores.stream()
                .collect(Collectors.toMap(
                        p -> p.getNombreProveedor().toUpperCase(),
                        p -> p
                ));
    }

    public void redirigirPeticion(PeticionEntregaDTO request, String proveedor) {
        String claveProveedor = (proveedor != null) ? proveedor.toUpperCase() : "PROPIO";

        ServicioLogisticaAdapter adapter = proveedores.get(claveProveedor);

        if (adapter == null) {
            throw new IllegalArgumentException("El proveedor de logística '" + proveedor + "' no está disponible.");
        }

        adapter.procesarEntrega(request);
    }
}
