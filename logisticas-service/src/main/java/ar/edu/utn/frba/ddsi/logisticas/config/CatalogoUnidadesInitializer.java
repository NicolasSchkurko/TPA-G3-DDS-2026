package ar.edu.utn.frba.ddsi.logisticas.config;

import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioUnidadesDeMedida;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoUnidadesInitializer {

    private static final Logger log = LoggerFactory.getLogger(CatalogoUnidadesInitializer.class);

    private final RepositorioUnidadesDeMedida repoUnidades;

    public CatalogoUnidadesInitializer(RepositorioUnidadesDeMedida repoUnidades) {
        this.repoUnidades = repoUnidades;
    }

    @PostConstruct
    public void inicializar() {
        try {
            for (UnidadDeMedida unidad : List.of(
                    UnidadDeMedida.UNIDADES,
                    UnidadDeMedida.KILOGRAMOS,
                    UnidadDeMedida.LITROS)) {
                if (!repoUnidades.existsById(unidad.getIdUnidad())) {
                    repoUnidades.save(unidad);
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo pre-cargar el catálogo de unidades de medida: {}", e.getMessage());
        }
    }
}
