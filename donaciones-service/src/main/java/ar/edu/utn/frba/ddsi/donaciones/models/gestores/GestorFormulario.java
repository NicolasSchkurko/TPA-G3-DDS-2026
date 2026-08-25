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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class GestorFormulario {
    private RepositorioDonaciones repositorioDonaciones;
    private RepositorioFormularios repositorioFormularios;

    public List<Donacion> procesarFormulario(Donante donante, List<Bien> bienesNormal, LocalDate fechaRealizacion) {

        Formulario formulario = new Formulario(donante, bienesNormal, fechaRealizacion);
        repositorioFormularios.save(formulario);

        DonacionFacade donacionFacade = new DonacionFacade(
                new SegmentadorDonaciones(),
                new AsignadorDonaciones()
        );

        List<Donacion> donacionesProcesadas = donacionFacade.crearDonaciones(formulario); //ejecuto segmentacion
        repositorioDonaciones.saveFormulario(donacionesProcesadas);

        return donacionesProcesadas;
    }
}
