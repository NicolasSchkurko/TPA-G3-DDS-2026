package ar.edu.utn.frba.ddsi.donaciones.config;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.CompatibilidadSemantica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AlgoritmosDeAsignacion.SubAtendidos;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.AsignadorDonaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.RepositorioDeResultadosMatchmaking;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsignacionConfig {

    @Bean
    public RepositorioDeResultadosMatchmaking repositorioMatchmaking() {
        return new RepositorioDeResultadosMatchmaking();
    }

    // TODO(equipo-asignacion): descomentar cuando se resuelva el constructor/API de AsignadorDonaciones
    // (hoy no tiene constructor sin argumentos ni método agregarAlgoritmo). Comentado temporalmente
    // el 2026-09-03 para poder compilar y avanzar con la persistencia de donaciones-service.
//    @Bean
//    public AsignadorDonaciones asignadorDonaciones() {
//        AsignadorDonaciones asignador = new AsignadorDonaciones();
//        asignador.agregarAlgoritmo(new CompatibilidadSemantica());
//        asignador.agregarAlgoritmo(new SubAtendidos());
//        return asignador;
//    }
}
