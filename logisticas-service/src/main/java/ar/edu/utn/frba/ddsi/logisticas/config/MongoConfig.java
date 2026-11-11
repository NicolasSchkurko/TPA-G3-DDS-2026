package ar.edu.utn.frba.ddsi.logisticas.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Pool y timeouts de Mongo dimensionados para el servicio (punto 70). Sin esto el driver usa
 * maxPoolSize=100 por instancia y no falla rápido si Atlas no responde.
 */
@Configuration
public class MongoConfig {

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientSettingsBuilderCustomizer(
            @Value("${mongodb.pool.max-size:20}") int maxPoolSize,
            @Value("${mongodb.pool.min-size:2}") int minPoolSize,
            @Value("${mongodb.server-selection-timeout-ms:5000}") long serverSelectionTimeoutMs,
            @Value("${mongodb.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${mongodb.socket-timeout-ms:10000}") int socketTimeoutMs) {
        return builder -> builder
            .applyToConnectionPoolSettings(pool -> pool
                .maxSize(maxPoolSize)
                .minSize(minPoolSize))
            .applyToClusterSettings(cluster -> cluster
                .serverSelectionTimeout(serverSelectionTimeoutMs, TimeUnit.MILLISECONDS))
            .applyToSocketSettings(socket -> socket
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(socketTimeoutMs, TimeUnit.MILLISECONDS));
    }
}
