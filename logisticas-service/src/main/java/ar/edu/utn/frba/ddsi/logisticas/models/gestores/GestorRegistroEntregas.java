package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.BienDTO;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.ItemEntrega;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.ItemEntrega.UnidadDeMedida;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioItemEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class GestorRegistroEntregas {

    private static final Logger log = LoggerFactory.getLogger(GestorRegistroEntregas.class);

    private final RepositorioItemEntrega repoItemEntrega;
    private final GestorCatalogoUbicaciones gestorCatalogoUbicaciones;

    public GestorRegistroEntregas(RepositorioItemEntrega repoItemEntrega,
                                  GestorCatalogoUbicaciones gestorCatalogoUbicaciones) {
        this.repoItemEntrega = repoItemEntrega;
        this.gestorCatalogoUbicaciones = gestorCatalogoUbicaciones;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EntregaDTO request) {
        validar(request);

        List<BienDTO> bienes = request.getDonacionResumen().getBienes();
        List<UUID> idsDonaciones = request.getDonacionResumen().getIdsDonaciones();
        Entidad entidadDestino = gestorCatalogoUbicaciones.resolverEntidad(request.getEntidadBeneficiaria());

        int registrados = 0;
        int repetidos = 0;

        for (int i = 0; i < bienes.size(); i++) {
            UUID idDonacion = idsDonaciones.get(i);
            if (repoItemEntrega.existsById(idDonacion)) {
                repetidos++;
                log.info("La donacion {} ya estaba registrada, el mensaje se repite y se omite", idDonacion);
                continue;
            }

            BienDTO bien = bienes.get(i);
            UnidadDeMedida unidad = mapearUnidadDeMedida(bien.getUnidadDeMedida());
            repoItemEntrega.save(new ItemEntrega(idDonacion, bien.getCantidad(), unidad, entidadDestino));
            registrados++;
        }

        log.info("Petición procesada: {} ítems registrados, {} repetidos omitidos", registrados, repetidos);
    }

    private void validar(EntregaDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La petición no puede ser nula");
        }
        if (request.getDonacionResumen() == null) {
            throw new IllegalArgumentException("La petición no trae el resumen de la donación");
        }
        if (request.getEntidadBeneficiaria() == null) {
            throw new IllegalArgumentException("La petición no trae la entidad beneficiaria");
        }

        List<BienDTO> bienes = request.getDonacionResumen().getBienes();
        List<UUID> idsDonaciones = request.getDonacionResumen().getIdsDonaciones();

        if (bienes == null) {
            throw new IllegalArgumentException("La lista de bienes de la donación no puede ser nula");
        }
        if (idsDonaciones == null) {
            throw new IllegalArgumentException("La lista de IDs de donaciones no puede ser nula");
        }
        if (bienes.size() != idsDonaciones.size()) {
            throw new IllegalArgumentException("La cantidad de bienes (" + bienes.size()
                    + ") no coincide con la cantidad de donaciones (" + idsDonaciones.size() + ")");
        }
    }

    private UnidadDeMedida mapearUnidadDeMedida(String unidadStr) {
        if (unidadStr == null) {
            throw new IllegalArgumentException("La unidad de medida no puede ser nula");
        }
        return switch (unidadStr.toUpperCase()) {
            case "UNIDADES", "UNIDAD" -> UnidadDeMedida.UNIDADES;
            case "KILOGRAMOS", "KG" -> UnidadDeMedida.KILOGRAMOS;
            case "LITROS", "L" -> UnidadDeMedida.LITROS;
            default -> throw new IllegalArgumentException("Unidad de medida no soportada: " + unidadStr);
        };
    }
}
