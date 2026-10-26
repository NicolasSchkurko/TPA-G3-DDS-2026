package ar.edu.utn.frba.ddsi.logisticas.mongoDB;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class MongoDBConnection {

    private final MongoDatabase database;

    public MongoDBConnection(
            MongoClient mongoClient,
            @Value("${spring.data.mongodb.database}") String databaseName) {

        this.database = mongoClient.getDatabase(databaseName);
    }

}