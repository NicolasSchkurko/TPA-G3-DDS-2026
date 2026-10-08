package ar.edu.utn.frba.ddsi.logisticas;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogisticaServiceApplication {
  public static void main(String[] args) {
    // Carga el .env del modulo antes de que arranque Spring, para que placeholders como
    // ${DB_PASSWORD} se resuelvan. En Docker las variables llegan por entorno y no hay .env:
    // ahi ignoreIfMissing() hace que esto no haga nada.
    //
    // Sin esto el servicio NO levanta fuera de Docker: application.properties no tiene default
    // para las credenciales a proposito, asi que sin la variable y sin el .env no hay con que
    // resolver el placeholder.
    Dotenv.configure().ignoreIfMissing().load().entries().forEach(entrada -> {
      if (System.getProperty(entrada.getKey()) == null
              && System.getenv(entrada.getKey()) == null) {
        System.setProperty(entrada.getKey(), entrada.getValue());
      }
    });

    SpringApplication.run(LogisticaServiceApplication.class, args);
  }
}