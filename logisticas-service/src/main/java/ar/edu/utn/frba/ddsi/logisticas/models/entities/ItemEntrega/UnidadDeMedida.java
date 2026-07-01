package ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega;

/**
 * NOTA DE NICO:
 * Me dio paja hacer un conversor de unidades pq me parecia poco flexible
 * Asi que hice que unidad de medida sea una clase que tiene en qué unidad
 * viene expresada una donación y sabe convertir esa unidad a peso para
 * chequear capacidad de los camiones.
 *
 */
public enum UnidadDeMedida {
    UNIDADES("Unidades", 1.0, 0.01),
    KILOGRAMOS("Kilogramos", 1.0, 0.001),
    LITROS("Litros", 1.0, 0.001);

    private final String nombre;
    private final double factorPesoKgPorUnidad;
    private final double factorVolumenM3PorUnidad;

    UnidadDeMedida(String nombre, double factorPesoKgPorUnidad, double factorVolumenM3PorUnidad) {
        this.nombre = nombre;
        this.factorPesoKgPorUnidad = factorPesoKgPorUnidad;
        this.factorVolumenM3PorUnidad = factorVolumenM3PorUnidad;
    }

    public String getNombre() {
        return nombre;
    }

    public double calcularPesoKg(Integer cantidad) {
        return cantidad * factorPesoKgPorUnidad;
    }

    public double calcularVolumenM3(Integer cantidad) {
        return cantidad * factorVolumenM3PorUnidad;
    }
}

