package ar.edu.utn.frba.ddsi.logisticas.models.gestores;

import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.DireccionDTO;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Ciudad;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Direccion;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Pais;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Direccion.Provincia;
import ar.edu.utn.frba.ddsi.logisticas.models.entities.Entidad.Entidad;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioCiudades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioDirecciones;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioEntidades;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioPaises;
import ar.edu.utn.frba.ddsi.logisticas.models.repositories.RepositorioProvincias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
public class GestorCatalogoUbicaciones {

    private static final Logger log = LoggerFactory.getLogger(GestorCatalogoUbicaciones.class);

    private final RepositorioEntidades repoEntidades;
    private final RepositorioDirecciones repoDirecciones;
    private final RepositorioCiudades repoCiudades;
    private final RepositorioProvincias repoProvincias;
    private final RepositorioPaises repoPaises;

    public GestorCatalogoUbicaciones(RepositorioEntidades repoEntidades,
                                     RepositorioDirecciones repoDirecciones,
                                     RepositorioCiudades repoCiudades,
                                     RepositorioProvincias repoProvincias,
                                     RepositorioPaises repoPaises) {
        this.repoEntidades = repoEntidades;
        this.repoDirecciones = repoDirecciones;
        this.repoCiudades = repoCiudades;
        this.repoProvincias = repoProvincias;
        this.repoPaises = repoPaises;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Entidad resolverEntidad(DireccionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("La entidad beneficiaria no trae dirección");
        }

        UUID idEntidad = dto.getIdEntidad();
        if (idEntidad == null) {
            throw new IllegalArgumentException("El ID de la entidad beneficiaria no puede ser nulo");
        }

        Optional<Entidad> yaExistente = repoEntidades.findById(idEntidad);
        if (yaExistente.isPresent()) {
            return yaExistente.get();
        }

        Direccion direccion = resolverDireccion(dto);

        try {
            return repoEntidades.save(new Entidad(idEntidad, direccion));
        } catch (DataIntegrityViolationException carrera) {
            log.info("La entidad {} ya fue registrada por otra instancia", idEntidad);
            return repoEntidades.findById(idEntidad)
                    .orElseThrow(() -> new IllegalStateException(
                            "La entidad " + idEntidad + " no se pudo resolver tras una carrera",
                            carrera));
        }
    }

    private Direccion resolverDireccion(DireccionDTO dto) {
        Pais pais = repoPaises.findByNombreIgnoreCase(dto.getPais())
                .orElseGet(() -> {
                    Pais nuevoPais = new Pais();
                    nuevoPais.setNombre(dto.getPais());
                    return repoPaises.save(nuevoPais);
                });

        Provincia provincia = repoProvincias
                .findByNombreIgnoreCaseAndPais_IdPais(dto.getProvincia(), pais.getIdPais())
                .orElseGet(() -> {
                    Provincia nuevaProvincia = new Provincia();
                    nuevaProvincia.setNombre(dto.getProvincia());
                    nuevaProvincia.setPais(pais);
                    return repoProvincias.save(nuevaProvincia);
                });

        Ciudad ciudad = repoCiudades
                .findByNombreIgnoreCaseAndProvincia_IdProvincia(dto.getCiudad(), provincia.getIdProvincia())
                .orElseGet(() -> {
                    Ciudad nuevaCiudad = new Ciudad(dto.getCiudad(), provincia);
                    return repoCiudades.save(nuevaCiudad);
                });

        Direccion direccion = new Direccion();
        direccion.setIdDireccion(UUID.randomUUID());
        direccion.setCalle1(dto.getCalleUno());
        direccion.setCalle2(dto.getCalleDos());
        direccion.setAltura(dto.getAltura());
        direccion.setPiso(dto.getPiso());
        direccion.setDepartamento(dto.getDepartamento());
        direccion.setCiudad(ciudad);
        direccion.setLatitud(dto.getLatitud());
        direccion.setLongitud(dto.getLongitud());

        return repoDirecciones.save(direccion);
    }
}
