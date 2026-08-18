package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.dto.donaciones.DonacionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.DireccionDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadBeneficiaria.NecesidadDTO;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorAdministradores;
import ar.edu.utn.frba.ddsi.donaciones.gestores.GestorEntidadesBeneficiarias;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.CategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.SubcategoriaBien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.ServicioMensaje.FabricaEstrategiasNotificacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Ciudad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Direccion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Pais;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.Provincia;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EntidadBeneficiariaService {

    private final GestorEntidadesBeneficiarias gestorEntidades;
    private final RepositorioDonaciones repositorioDonaciones;
    private final FabricaEstrategiasNotificacion fabricaEstrategias;
    private final GestorAdministradores gestorAdministradores;


    public EntidadBeneficiariaService(GestorEntidadesBeneficiarias gestorEntidades,
                                      RepositorioDonaciones repositorioDonaciones,
                                      FabricaEstrategiasNotificacion fabricaEstrategias,
                                      GestorAdministradores gestorAdministradores) {
        this.gestorEntidades = gestorEntidades;
        this.repositorioDonaciones = repositorioDonaciones;
        this.fabricaEstrategias = fabricaEstrategias;
        this.gestorAdministradores = gestorAdministradores;

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
        EntidadBeneficiaria existente = gestorEntidades.obtenerEntidad(id);
        if (existente == null) {
            throw new IllegalArgumentException("No se encontró la entidad con ID: " + id);
        }

        existente.getPersonaJuridica()
                .setMediosDeContacto(entidadActualizada.getPersonaJuridica().getMediosDeContacto());
        existente.setDireccion(entidadActualizada.getDireccion());

        gestorEntidades.modificarEntidad(id, existente);
        return existente;
    }

    public void eliminarEntidad(UUID id) {
        gestorEntidades.darDeBajaEntidad(id);
    }

    // --- OPERACIONES CRUD NECESIDADES ---

    public List<Necesidad> obtenerNecesidades(UUID idEntidad) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(idEntidad);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad");
        }

        return entidad.getNecesidades();
    }

    public Necesidad agregarNecesidad(UUID idEntidad, Necesidad necesidad) {
        gestorEntidades.agregarNecesidadAEntidad(idEntidad, necesidad);
        return necesidad;
    }

    public void eliminarNecesidad(UUID idEntidad, UUID idNecesidad) {
        EntidadBeneficiaria entidad = gestorEntidades.obtenerEntidad(idEntidad);
        if (entidad == null) {
            throw new IllegalArgumentException("No se encontró la entidad");
        }

        Necesidad necesidad = entidad.buscarNecesidadPorId(idNecesidad)
                                     .orElseThrow(() -> new IllegalArgumentException("No se encontró la necesidad"));

        entidad.eliminarNecesidad(necesidad);
        gestorEntidades.modificarEntidad(idEntidad, entidad);
    }

    // --- OTROS MÉTODOS ---

    public List<Donacion> obtenerDonaciones(UUID idEntidad) {
        return gestorEntidades.obtenerDonacionesDeEntidad(idEntidad);
    }
}