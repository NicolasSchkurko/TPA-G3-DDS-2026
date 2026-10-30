package ar.edu.utn.frba.ddsi.logisticas.models.entities.EventoLogistica;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventoLogisticaSecuenciaTest {

    @BeforeEach
    void setUp() {
        // Asegurar que mongoTemplate estático esté en null para emular test unitario sin Spring Mongo
        Secuencia.setMongoTemplate(null);
    }

    @Test
    @DisplayName("Crear EventoLogistica sin MongoTemplate inicializado no debe lanzar NPE y debe autogenerar ID")
    void crearEventoSinMongoTemplateNoLanzaNPE() {
        assertDoesNotThrow(() -> {
            EventoLogistica evento1 = new EventoLogistica("TEST_EVENTO", "REF_123", LocalDateTime.now(), "Justificacion");
            assertNotNull(evento1.getId(), "El ID no debe ser nulo");

            EventoLogistica evento2 = new EventoLogistica("TEST_EVENTO_2", "{}");
            assertNotNull(evento2.getId(), "El ID no debe ser nulo");
            assertNotEquals(evento1.getId(), evento2.getId(), "Los IDs generados deben ser distintos");
        });
    }
}
