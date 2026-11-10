package ar.edu.utn.frba.ddsi.logisticas.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.MongoClientSettings;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Punto 70: pool y timeouts de Mongo")
class MongoConfigTest {

  @Test
  @DisplayName("el customizer aplica maxPoolSize, mínimos y timeouts")
  void aplicaPoolYTimeouts() {
    MongoClientSettings.Builder builder = MongoClientSettings.builder();
    new MongoConfig()
        .mongoClientSettingsBuilderCustomizer(20, 2, 5000, 6000, 7000)
        .customize(builder);
    MongoClientSettings settings = builder.build();

    assertThat(settings.getConnectionPoolSettings().getMaxSize()).isEqualTo(20);
    assertThat(settings.getConnectionPoolSettings().getMinSize()).isEqualTo(2);
    assertThat(settings.getClusterSettings().getServerSelectionTimeout(TimeUnit.MILLISECONDS))
        .isEqualTo(5000);
    assertThat(settings.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS))
        .isEqualTo(6000);
    assertThat(settings.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS))
        .isEqualTo(7000);
  }
}
