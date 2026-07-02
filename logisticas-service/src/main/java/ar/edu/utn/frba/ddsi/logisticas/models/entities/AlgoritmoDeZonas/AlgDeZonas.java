package ar.edu.utn.frba.ddsi.logisticas.models.entities.AlgoritmoDeZonas;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.Camion.Camion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.PesoPaquete.PesoTotal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlgDeZonas {
    // cuando nos llega una lista con bienes, lo primero q deberiammos hacer es
    // separar las donaciones por zonas, o sea zona oeste, sur, este y norte (si es que
    // complemplamos que sea bs as solo, por ejemplo) y dsp con cada bien, vamos a tener que
    // armar distintos "paquetes" sumando los pesos para asignarlos a distintos camiones

    // podriamos hacer un algortimo que sea cargar los camiones unicamente por el peso, o sea
    // hasta que se llenen, sin importar las ubiaciones, y nos olvidamos de la eficiencia


     //Agrupa por ciudad de destino y luego subdivide en tandas según la capacidad del camión.

    public Map<String, List<List<ItemEntrega>>> ejecutarPorCiudad(List<ItemEntrega> items, Camion camionReferencia) {

        // 1. Agrupamos por el nombre de la ciudad directo usando Streams
        Map<String, List<ItemEntrega>> itemsPorCiudad = items.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getEntidadDestino().getDireccionDestino().getCiudad().getNombre()
                ));

        Map<String, List<List<ItemEntrega>>> resultadoFinal = new HashMap<>();

        // 2. Para cada ciudad, armamos los paquetes por peso correspondientes
        return itemsPorCiudad.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entrada -> armarTandasPorPeso(entrada.getValue(), camionReferencia)
                ));
    }

    private List<List<ItemEntrega>> armarTandasPorPeso(List<ItemEntrega> items, Camion camion) {
        // Usamos una estructura mutable para acumular el resultado, pero el recorrido es funcional
        List<List<ItemEntrega>> tandas = new ArrayList<>();

        // Inicializamos la primera tanda vacía
        tandas.add(new ArrayList<>());

        // Usamos .forEach del Stream para ir construyendo las particiones dinámicamente
        items.stream().forEach(item -> {
            List<ItemEntrega> tandaActual = tandas.get(tandas.size() - 1);

            // Evaluamos el peso tentativo con el nuevo ítem
            List<ItemEntrega> tandaTentativa = new ArrayList<>(tandaActual);
            tandaTentativa.add(item);

            PesoTotal totales = new PesoTotal(tandaTentativa);

            if (totales.entraEn(camion)) {
                tandaActual.add(item);
            } else {
                // Si no entra, creamos una nueva tanda con el ítem actual y la agregamos a la lista general
                List<ItemEntrega> nuevaTanda = new ArrayList<>();
                nuevaTanda.add(item);
                tandas.add(nuevaTanda);
            }
        });

        // Limpieza por si la primera tanda quedó vacía por algún motivo marginal
        if (tandas.get(0).isEmpty()) {
            tandas.remove(0);
        }

        return tandas;
    }
}