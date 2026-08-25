package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioFormularios;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestorFormulario {
    private RepositorioDonaciones repositorioDonaciones;
    private RepositorioFormularios repositorioFormularios;

    // Constructor para inyección de dependencias
    public GestorFormulario(RepositorioDonaciones repositorioDonaciones, RepositorioFormularios repositorioFormularios) {
        this.repositorioDonaciones = repositorioDonaciones;
        this.repositorioFormularios = repositorioFormularios;
    }

    // --- Lógica de Negocio ---
    public List<Donacion> procesarFormulario(Donante donante, List<Bien> bienesNormal, LocalDate fechaRealizacion) {

        Formulario formulario = new Formulario(donante, bienesNormal, fechaRealizacion);
        repositorioFormularios.guardar(formulario);

        DonacionFacade donacionFacade = new DonacionFacade(
            new SegmentadorDonaciones(),
            new AsignadorDonaciones()
        );

        List<Donacion> donacionesProcesadas = donacionFacade.crearDonaciones(formulario); //ejecuto segmentacion
        repositorioDonaciones.guardarDonaciones(donacionesProcesadas);

        return donacionesProcesadas;
    }

    // --- Métodos CRUD para Formulario ---

    // Create (adicional al procesarFormulario si se necesita guardar de forma aislada)
    public void guardarFormulario(Formulario formulario) {
        repositorioFormularios.guardar(formulario);
    }

    // Read
    public List<Formulario> obtenerTodosLosFormularios() {
        return repositorioFormularios.obtenerTodos();
    }

    public Optional<Formulario> obtenerFormularioPorId(UUID id) {
        return repositorioFormularios.buscarPorId(id); // o obtenerPorId según tu RepositorioFormularios
    }


    // Delete
    public void eliminarFormulario(UUID id) {
        repositorioFormularios.eliminarPorId(id);
    }
}