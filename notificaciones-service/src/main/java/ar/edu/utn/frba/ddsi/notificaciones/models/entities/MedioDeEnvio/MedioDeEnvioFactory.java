package ar.edu.utn.frba.ddsi.notificaciones.models.entities.MedioDeEnvio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MedioDeEnvioFactory {
    private Map<String, MedioDeEnvio> medios;

    @Autowired
    public MedioDeEnvioFactory(Map<String, MedioDeEnvio> medios) {
        this.medios = medios;
    }

    public MedioDeEnvio mapearAMedioEnvio(String tipo) {
        MedioDeEnvio medio = medios.get(tipo);
        if (medio == null) {
            throw new IllegalArgumentException("Tipo desconocido: " + tipo);
        }
        return medio;
    }
}

