package ar.edu.utn.frba.ddsi.donaciones.models.gestores;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.DonacionFacade;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Formulario.Formulario;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.SegmentadorDonaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioFormularios;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GestorFormulario {

    private final RepositorioFormularios repositorioFormularios;

    // Constructor para inyección de dependencias
    public GestorFormulario(RepositorioFormularios repositorioFormularios) {
        this.repositorioFormularios = repositorioFormularios;
    }

    // --- Lógica de Negocio ---

    /**
     * Toma un formulario ya creado y ejecuta la segmentación a través de la Fachada.
     * Devuelve las Donaciones segmentadas pero NO altera el formulario original.
     */
    public List<Donacion> procesarFormulario(Formulario formulario) {
        DonacionFacade donacionFacade = new DonacionFacade(
            new SegmentadorDonaciones(), null
        );

        // Ejecutamos la segmentación y devolvemos el resultado
        return donacionFacade.crearDonaciones(formulario);
    }
}