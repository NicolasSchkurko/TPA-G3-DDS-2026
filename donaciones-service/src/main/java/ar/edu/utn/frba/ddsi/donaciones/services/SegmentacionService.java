package ar.edu.utn.frba.ddsi.donaciones.services;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.PersonaDonante;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SegmentacionService {
    private final SegmentadorDonaciones segmentador;

    public SegmentacionService(SegmentadorDonaciones segmentador) {
        this.segmentador = segmentador;
    }

    public List<Donacion> ejecutarSegmentacion(PersonaDonante donante, List<Bien> donacionesNoSegmentadas) {
        return segmentador.segmentar(donante, donacionesNoSegmentadas);
    }
}
