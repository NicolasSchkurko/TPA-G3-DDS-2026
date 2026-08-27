package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EntidadBeneficiariaService {

    private final GestorEntidadesBeneficiarias gestorEntidades;

    public EntidadBeneficiariaService(GestorEntidadesBeneficiarias gestorEntidades) {
        this.gestorEntidades = gestorEntidades;
    }

    // --- OPERACIONES CRUD ENTIDADES ---

    public List<EntidadBeneficiaria> obtenerTodas() {
        return gestorEntidades.listarTodasLasEntidades();
    }

    public EntidadBeneficiaria obtenerEntidadPorId(UUID id) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(id);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);
        }
        return entidad;
    }

    public EntidadBeneficiaria registrarEntidad(EntidadBeneficiaria entidad) {
        gestorEntidades.registrarEntidad(entidad);
        return entidad;
    }

    public EntidadBeneficiaria actualizarEntidad(UUID id, EntidadBeneficiaria entidadActualizada) {
        // Toda la lógica de negocio se movió al gestor
        return gestorEntidades.modificarEntidad(id, entidadActualizada);
    }

    public void eliminarEntidad(UUID id) {
        gestorEntidades.darDeBajaEntidad(id);
    }

    // --- OPERACIONES CRUD NECESIDADES ---

    public List<Necesidad> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = obtenerEntidadPorId(idEntidad);
        return entidad.getNecesidades();
    }

    public Necesidad agregarNecesidad(UUID idEntidad, Necesidad necesidad) {
        gestorEntidades.agregarNecesidadAEntidad(idEntidad, necesidad);
        return necesidad;
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        // La búsqueda, eliminación y persistencia ahora la hace el gestor
        gestorEntidades.eliminarNecesidadDeEntidad(idEntidad, idNecesidad);
    }

    // --- OTROS MÉTODOS ---

    public List<Donacion> obtenerDonaciones(UUID idEntidad) {
        return gestorEntidades.obtenerDonacionesDeEntidad(idEntidad);
    }
}