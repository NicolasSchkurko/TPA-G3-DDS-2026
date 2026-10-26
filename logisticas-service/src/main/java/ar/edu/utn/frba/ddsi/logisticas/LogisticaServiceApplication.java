package ar.edu.utn.frba.ddsi.logisticas;

import ar.edu.utn.frba.ddsi.logisticas.mongoDB.MongoDBConnection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogisticaServiceApplication {

  public static void main(String[] args) {

    Dotenv dotenv = Dotenv.configure()
            .directory("./logisticas-service")
            .ignoreIfMissing()
            .load();

    dotenv.entries().forEach(entrada -> {
      if (System.getProperty(entrada.getKey()) == null
              && System.getenv(entrada.getKey()) == null) {
        System.setProperty(entrada.getKey(), entrada.getValue());
      }
    });

    System.out.println("Directorio de trabajo: " + System.getProperty("user.dir"));
    System.out.println("MONGODB_URI cargada: "
            + (System.getProperty("MONGODB_URI") != null ? "Sí" : "No"));
    System.out.println("MONGODB_DB_NAME cargada: "
            + System.getProperty("MONGODB_DB_NAME"));

    ConfigurableApplicationContext context =
            SpringApplication.run(LogisticaServiceApplication.class, args);

    try {
      MongoDBConnection conexion =
              context.getBean(MongoDBConnection.class);

      MongoDatabase database = conexion.getDatabase();

      database.runCommand(new Document("ping", 1));

      System.out.println("Conexión a MongoDB exitosa.");

    } catch (Exception e) {
      System.err.println(
              "No se pudo conectar con MongoDB: " + e.getMessage()
      );
    }
  }
}